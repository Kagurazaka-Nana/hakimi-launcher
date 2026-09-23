package com.minecraft.launcher.download.mojang;

import com.minecraft.launcher.model.version.assetindex.AssetIndex;
import com.minecraft.launcher.model.version.assetindex.AssetIndexFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetServiceTest {

    @TempDir
    Path dir;

    // SHA-1 是 Mojang 资源协议规定的内容寻址标识（完整性语义，非安全用途），算法收敛在 Checksums 常量
    private static String sha1(String content) throws Exception {
        MessageDigest md = MessageDigest.getInstance(Checksums.MOJANG_DIGEST);
        return HexFormat.of().formatHex(md.digest(content.getBytes(StandardCharsets.UTF_8)));
    }

    private String indexJson() throws Exception {
        return """
                {"objects":{
                  "minecraft/lang/aa.json":{"hash":"%s","size":3},
                  "minecraft/lang/bb.json":{"hash":"%s","size":5}
                }}
                """.formatted(sha1("AAA"), sha1("BBBBB"));
    }

    private AssetIndex pointer(String indexJson) throws Exception {
        return new AssetIndex("17", sha1(indexJson), indexJson.length(), 8, "https://x.test/17.json");
    }

    private AssetService service(StubFileDownloader stub) {
        return new AssetService(new MojangProvider(), stub, new GameLayout(dir));
    }

    @Test
    void downloadsIndexAndCachesIt() throws Exception {
        String json = indexJson();
        StubFileDownloader stub = new StubFileDownloader().respond("https://x.test/17.json", json);

        AssetIndexFile index = service(stub).loadIndex(pointer(json));
        service(stub).loadIndex(pointer(json));

        assertEquals(2, index.getObjects().size());
        assertEquals(1, stub.hitCount("https://x.test/17.json"), "索引应被缓存复用");
    }

    @Test
    void corruptIndexIsRedownloaded() throws Exception {
        String json = indexJson();
        Path indexFile = dir.resolve("assets/indexes/17.json");
        Files.createDirectories(indexFile.getParent());
        Files.writeString(indexFile, "GARBAGE");
        StubFileDownloader stub = new StubFileDownloader().respond("https://x.test/17.json", json);

        AssetIndexFile index = service(stub).loadIndex(pointer(json));

        assertEquals(2, index.getObjects().size());
        assertEquals(1, stub.hitCount("https://x.test/17.json"));
    }

    @Test
    void planSkipsSatisfiedObjectsAndUsesCdnCandidates() throws Exception {
        String json = indexJson();
        StubFileDownloader stub = new StubFileDownloader().respond("https://x.test/17.json", json);
        AssetService service = service(stub);
        AssetIndexFile index = service.loadIndex(pointer(json));

        // 预置一个大小匹配的已存在对象
        String hashA = sha1("AAA");
        Path existing = dir.resolve("assets/objects").resolve(hashA.substring(0, 2)).resolve(hashA);
        Files.createDirectories(existing.getParent());
        Files.writeString(existing, "AAA");

        List<FileEntry> plan = service.planObjects(index, false);

        assertEquals(1, plan.size(), "大小匹配的已有对象应跳过");
        FileEntry entry = plan.get(0);
        String hashB = sha1("BBBBB");
        assertEquals(List.of("https://resources.download.minecraft.net/" + hashB.substring(0, 2) + "/" + hashB),
                entry.candidateUrls());
        assertEquals(hashB, entry.sha1());
        assertEquals(5, entry.size());
    }

    @Test
    void verifyExistingCatchesSizeCollisionCorruption() throws Exception {
        String json = indexJson();
        StubFileDownloader stub = new StubFileDownloader().respond("https://x.test/17.json", json);
        AssetService service = service(stub);
        AssetIndexFile index = service.loadIndex(pointer(json));

        String hashA = sha1("AAA");
        Path corrupt = dir.resolve("assets/objects").resolve(hashA.substring(0, 2)).resolve(hashA);
        Files.createDirectories(corrupt.getParent());
        Files.writeString(corrupt, "XXX"); // 大小相同内容不同

        assertEquals(1, service.planObjects(index, false).size(), "默认只看大小 → 漏检");
        assertEquals(2, service.planObjects(index, true).size(), "verifyExisting 应查出坏文件");
    }

    @Test
    void indexDownloadFailureThrows() throws Exception {
        StubFileDownloader stub = new StubFileDownloader();
        AssetService service = service(stub);
        assertThrows(IOException.class, () -> service.loadIndex(pointer("{}")));
    }
}
