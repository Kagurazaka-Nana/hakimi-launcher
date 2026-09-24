package com.minecraft.launcher.download.mojang;

import com.minecraft.launcher.download.FileDownloader;
import com.minecraft.launcher.model.version.assetindex.AssetIndex;
import com.minecraft.launcher.model.version.assetindex.AssetIndexFile;
import com.minecraft.launcher.util.JsonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * 资源管线（docs/DownloadPipeline.md §1.4、§3.0 assets 行）：
 * 先确保索引文件存在且 sha1 正确（坏则删除重下），再对 objects 做差集——
 * 默认按「存在 + 大小匹配」跳过（30 万文件全量哈希太贵），可开 verifyExisting 做完整校验。
 */
public final class AssetService {

    private final DownloadProvider provider;
    private final FileDownloader downloader;
    private final GameLayout layout;

    public AssetService(DownloadProvider provider, FileDownloader downloader, GameLayout layout) {
        this.provider = provider;
        this.downloader = downloader;
        this.layout = layout;
    }

    /** 加载（必要时下载/修复）资源索引文件。 */
    public AssetIndexFile loadIndex(AssetIndex pointer) throws IOException {
        Path target = layout.assetIndex(pointer.getId());
        if (Files.isRegularFile(target)) {
            if (Checksums.matches(target, Checksums.MOJANG_DIGEST, pointer.getSha1())) {
                return JsonUtils.readValue(target, AssetIndexFile.class);
            }
            Files.deleteIfExists(target); // 坏索引，重下
        }
        IOException last = null;
        for (String url : provider.injectURLCandidates(pointer.getUrl())) {
            Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
            try {
                Files.createDirectories(target.toAbsolutePath().getParent());
                downloader.downloadBlocking(url, tmp);
                if (!Checksums.matches(tmp, Checksums.MOJANG_DIGEST, pointer.getSha1())) {
                    throw new IOException("资源索引摘要不匹配: " + url);
                }
                AssetIndexFile index = JsonUtils.readValue(tmp, AssetIndexFile.class);
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
                return index;
            } catch (IOException | RuntimeException e) {
                Files.deleteIfExists(tmp);
                last = e instanceof IOException ioe ? ioe : new IOException(e);
            }
        }
        throw new IOException("资源索引下载失败: " + pointer.getId(), last);
    }

    /** 计算缺失/损坏对象的下载清单（虚拟路径顺序稳定）。 */
    public List<FileEntry> planObjects(AssetIndexFile index, boolean verifyExisting) throws IOException {
        List<FileEntry> out = new ArrayList<>();
        if (index.getObjects() == null) {
            return out;
        }
        for (AssetIndexFile.AssetObject obj : index.getObjects().values()) {
            Path target = layout.assetObject(obj.location());
            if (isSatisfied(target, obj, verifyExisting)) {
                continue;
            }
            out.add(new FileEntry(
                    provider.getAssetObjectCandidates(obj.location()),
                    target,
                    obj.getHash(),
                    obj.getSize() == null ? -1 : obj.getSize()));
        }
        return out;
    }

    private boolean isSatisfied(Path target, AssetIndexFile.AssetObject obj, boolean verifyExisting) throws IOException {
        if (!Files.isRegularFile(target)) {
            return false;
        }
        if (verifyExisting) {
            return Checksums.matches(target, Checksums.MOJANG_DIGEST, obj.getHash());
        }
        return obj.getSize() == null || obj.getSize() == Files.size(target);
    }
}
