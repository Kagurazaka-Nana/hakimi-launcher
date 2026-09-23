package com.minecraft.launcher.download.mojang;

import com.minecraft.launcher.model.rule.RuleEvaluator;
import com.minecraft.launcher.model.version.libraries.Artifact;
import com.minecraft.launcher.model.version.libraries.Download;
import com.minecraft.launcher.model.version.libraries.Library;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 按规则与平台挑选需要下载的构件（docs/DownloadPipeline.md §1.3）：
 * rules 过 RuleEvaluator；natives 按当前 OS 键取 classifier（值支持 {arch} 占位符）；
 * URL 解析顺序：artifact.url → library.url + path → 默认 libraries 仓库。
 */
public final class LibraryFilter {

    private final RuleEvaluator evaluator;

    public LibraryFilter(RuleEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    public List<LibraryDownload> select(List<Library> libraries) {
        List<LibraryDownload> out = new ArrayList<>();
        if (libraries == null) {
            return out;
        }
        for (Library lib : libraries) {
            if (!evaluator.evaluate(lib.getRules())) {
                continue;
            }
            Download dl = lib.getDownloads();
            if (dl == null) {
                // 旧式条目：仅 Maven 坐标 + 可选仓库 url（Forge 等）
                String path = mavenPath(lib.getName());
                String base = lib.getUrl() != null ? lib.getUrl() : MojangProvider.LIBRARIES_BASE;
                out.add(new LibraryDownload(lib.getName(), path, base + path, null, -1, false, List.of()));
                continue;
            }
            if (dl.getArtifact() != null) {
                out.add(toDownload(lib, dl.getArtifact(), false));
            }
            Artifact nativeArtifact = pickNative(lib, dl);
            if (nativeArtifact != null) {
                out.add(toDownload(lib, nativeArtifact, true));
            }
        }
        return out;
    }

    private Artifact pickNative(Library lib, Download dl) {
        if (lib.getNatives() == null || dl.getClassifiers() == null || dl.getClassifiers().isEmpty()) {
            return null;
        }
        String osName = evaluator.getRuleContext().getOsName();
        String classifier = lib.getNatives().get(osName);
        if (classifier == null) {
            return null;
        }
        classifier = classifier.replace("{arch}", evaluator.getRuleContext().getArch());
        return dl.getClassifiers().get(classifier);
    }

    private LibraryDownload toDownload(Library lib, Artifact artifact, boolean isNative) {
        String url = resolveUrl(lib, artifact);
        long size = artifact.getSize() == null ? -1 : artifact.getSize();
        List<String> exclude = isNative && lib.getExtract() != null && lib.getExtract().getExclude() != null
                ? lib.getExtract().getExclude()
                : List.of();
        return new LibraryDownload(lib.getName(), artifact.getPath(), url, artifact.getSha1(), size, isNative, exclude);
    }

    private String resolveUrl(Library lib, Artifact artifact) {
        if (artifact.getUrl() != null && !artifact.getUrl().isBlank()) {
            return artifact.getUrl();
        }
        String base = lib.getUrl() != null && !lib.getUrl().isBlank() ? lib.getUrl() : MojangProvider.LIBRARIES_BASE;
        return base + artifact.getPath();
    }

    /** group:artifact:version[:classifier] → group/path/artifact/version/artifact-version[-classifier].jar */
    static String mavenPath(String name) {
        String[] parts = name.split(":");
        if (parts.length < 3) {
            throw new IllegalArgumentException("非法 Maven 坐标: " + name);
        }
        String group = parts[0].replace('.', '/');
        String artifactId = parts[1];
        String version = parts[2];
        String classifier = parts.length >= 4 ? "-" + parts[3] : "";
        return group + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + classifier + ".jar";
    }
}
