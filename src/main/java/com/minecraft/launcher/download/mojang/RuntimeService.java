package com.minecraft.launcher.download.mojang;

import com.fasterxml.jackson.core.type.TypeReference;
import com.minecraft.launcher.download.FileDownloader;
import com.minecraft.launcher.model.version.libraries.Artifact;
import com.minecraft.launcher.util.JsonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Java runtime 自动安装（docs/DownloadPipeline.md §1.5）：
 * all.json（平台→组件目录，URL 自带哈希固定版本）→ manifest.json（按 sha1 校验缓存）
 * → raw 文件清单 → 安装到 runtime/&lt;component&gt;/&lt;platform&gt;/，POSIX 平台补执行位。
 */
public final class RuntimeService {

    /** 安装计划：文件清单 + 运行时目录 + 需要执行位的相对路径。 */
    public record RuntimePlan(List<FileEntry> files, Path runtimeDir, List<String> executablePaths) {
        public RuntimePlan {
            files = List.copyOf(files);
            executablePaths = List.copyOf(executablePaths);
        }
    }

    private static final TypeReference<Map<String, Map<String, List<JavaRuntimeModels.CatalogEntry>>>> CATALOG_TYPE =
            new TypeReference<>() {};

    private final DownloadProvider provider;
    private final FileDownloader downloader;
    private final GameLayout layout;
    private final Path cacheDir;

    public RuntimeService(DownloadProvider provider, FileDownloader downloader, GameLayout layout, Path cacheDir) {
        this.provider = provider;
        this.downloader = downloader;
        this.layout = layout;
        this.cacheDir = cacheDir;
    }

    /** Mojang runtime 目录的平台键（osName/arch 来自 RuleContext/PlatformUtil 归一化值）。 */
    public static String platformKey(String osName, String arch) {
        return switch (osName) {
            case "windows" -> switch (arch) {
                case "x86" -> "windows-x86";
                case "arm64" -> "windows-arm64";
                default -> "windows-x64";
            };
            case "osx" -> "arm64".equals(arch) ? "mac-os-arm64" : "mac-os";
            case "linux" -> switch (arch) {
                case "x86" -> "linux-i386";
                case "arm64" -> "linux-arm64";
                default -> "linux";
            };
            default -> throw new IllegalArgumentException("未知平台: " + osName + "-" + arch);
        };
    }

    public RuntimePlan plan(String component, String osName, String arch) throws IOException {
        String platform = platformKey(osName, arch);
        Map<String, Map<String, List<JavaRuntimeModels.CatalogEntry>>> catalog = fetchCatalog();
        // 组件值是数组（多个可用构建）：取第一个带 manifest 的条目
        JavaRuntimeModels.CatalogEntry entry = catalog.getOrDefault(platform, Map.of())
                .getOrDefault(component, List.of()).stream()
                .filter(e -> e != null && e.getManifest() != null)
                .findFirst().orElse(null);
        if (entry == null) {
            throw new IOException("runtime 目录中找不到组件 " + component + " 的平台 " + platform);
        }
        JavaRuntimeModels.Manifest manifest = fetchManifest(entry.getManifest());

        Path runtimeDir = layout.runtime(component, platform);
        List<FileEntry> files = new ArrayList<>();
        List<String> executables = new ArrayList<>();
        for (Map.Entry<String, JavaRuntimeModels.Manifest.FileEntry> e : manifest.getFiles().entrySet()) {
            JavaRuntimeModels.Manifest.FileEntry file = e.getValue();
            if (!file.isFile()) {
                continue;
            }
            Artifact raw = file.raw();
            if (raw == null || raw.getUrl() == null) {
                continue;
            }
            files.add(new FileEntry(
                    provider.injectURLCandidates(raw.getUrl()),
                    runtimeDir.resolve(e.getKey()),
                    raw.getSha1(),
                    raw.getSize() == null ? -1 : raw.getSize()));
            if (file.isExecutable()) {
                executables.add(e.getKey());
            }
        }
        return new RuntimePlan(files, runtimeDir, executables);
    }

    /** POSIX 平台为清单标记的可执行文件补执行位（Windows 上 no-op）。 */
    public void applyExecutableBits(RuntimePlan plan) throws IOException {
        if (!Files.getFileStore(plan.runtimeDir().toAbsolutePath().getRoot()).supportsFileAttributeView("posix")) {
            return;
        }
        for (String relative : plan.executablePaths()) {
            Path file = plan.runtimeDir().resolve(relative);
            if (Files.isRegularFile(file)) {
                Set<PosixFilePermission> perms = Files.getPosixFilePermissions(file);
                if (!perms.contains(PosixFilePermission.OWNER_EXECUTE)) {
                    perms.add(PosixFilePermission.OWNER_EXECUTE);
                    perms.add(PosixFilePermission.GROUP_EXECUTE);
                    perms.add(PosixFilePermission.OTHERS_EXECUTE);
                    Files.setPosixFilePermissions(file, perms);
                }
            }
        }
    }

    private Map<String, Map<String, List<JavaRuntimeModels.CatalogEntry>>> fetchCatalog() throws IOException {
        Path target = cacheDir.resolve("java-runtime-catalog.json");
        if (Files.isRegularFile(target)) {
            return JsonUtils.readValue(target, CATALOG_TYPE);
        }
        IOException last = null;
        for (String url : provider.getJavaRuntimeCatalogUrls()) {
            Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
            try {
                Files.createDirectories(cacheDir);
                downloader.downloadBlocking(url, tmp);
                Map<String, Map<String, List<JavaRuntimeModels.CatalogEntry>>> catalog = JsonUtils.readValue(tmp, CATALOG_TYPE);
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
                return catalog;
            } catch (IOException | RuntimeException e) {
                Files.deleteIfExists(tmp);
                last = e instanceof IOException ioe ? ioe : new IOException(e);
            }
        }
        throw new IOException("Java runtime 目录拉取失败", last);
    }

    private JavaRuntimeModels.Manifest fetchManifest(JavaRuntimeModels.CatalogEntry.ManifestPointer pointer) throws IOException {
        Path target = cacheDir.resolve(pointer.getSha1() + ".manifest.json");
        if (Files.isRegularFile(target) && Checksums.matches(target, Checksums.MOJANG_DIGEST, pointer.getSha1())) {
            return JsonUtils.readValue(target, JavaRuntimeModels.Manifest.class);
        }
        IOException last = null;
        for (String url : provider.injectURLCandidates(pointer.getUrl())) {
            Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
            try {
                Files.createDirectories(cacheDir);
                downloader.downloadBlocking(url, tmp);
                if (!Checksums.matches(tmp, Checksums.MOJANG_DIGEST, pointer.getSha1())) {
                    throw new IOException("runtime manifest 摘要不匹配: " + url);
                }
                JavaRuntimeModels.Manifest manifest = JsonUtils.readValue(tmp, JavaRuntimeModels.Manifest.class);
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
                return manifest;
            } catch (IOException | RuntimeException e) {
                Files.deleteIfExists(tmp);
                last = e instanceof IOException ioe ? ioe : new IOException(e);
            }
        }
        throw new IOException("runtime manifest 拉取失败", last);
    }
}
