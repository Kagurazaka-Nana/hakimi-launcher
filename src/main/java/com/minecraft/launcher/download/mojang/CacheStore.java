package com.minecraft.launcher.download.mojang;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 中央缓存（参考 HMCL CacheRepository）：镜像 .minecraft 相对布局存放已校验文件，
 * 安装时优先硬链接进游戏目录（失败降级复制），实现多实例共享 assets/libraries、重复安装零流量。
 */
public final class CacheStore {

    private final Path root;

    public CacheStore(Path root) {
        this.root = root;
    }

    public Path cacheFile(String relativePath) {
        return root.resolve(relativePath);
    }

    /** 缓存中是否已有该相对路径且摘要匹配（expectedSha1 为空则只看存在性）。 */
    public boolean contains(String relativePath, String expectedSha1) throws IOException {
        Path file = cacheFile(relativePath);
        return Files.isRegularFile(file) && Checksums.matches(file, Checksums.MOJANG_DIGEST, expectedSha1);
    }

    /** 把已校验的临时文件收编进缓存（原子移动）。 */
    public void ingest(String relativePath, Path verifiedTmp) throws IOException {
        Path target = cacheFile(relativePath);
        Files.createDirectories(target.toAbsolutePath().getParent());
        Files.move(verifiedTmp, target, StandardCopyOption.REPLACE_EXISTING);
    }

    /** 把缓存文件链接到游戏目录目标（硬链接失败降级复制；目标已存在则替换）。 */
    public void linkInto(String relativePath, Path target) throws IOException {
        Path cached = cacheFile(relativePath);
        Files.createDirectories(target.toAbsolutePath().getParent());
        Files.deleteIfExists(target);
        try {
            Files.createLink(target, cached);
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            Files.copy(cached, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
