package com.minecraft.launcher.download.mojang;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 中央缓存行为：收编/命中/坏摘要；两个游戏目录共享缓存时第二个零网络下载。 */
class CacheStoreTest {

    @TempDir
    Path dir;

    // SHA-1 为 Mojang 协议内容寻址标识（完整性语义，非安全用途）
    private static String sha1(String content) throws Exception {
        MessageDigest md = MessageDigest.getInstance(Checksums.MOJANG_DIGEST);
        return HexFormat.of().formatHex(md.digest(content.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void ingestThenLinkServesContent() throws Exception {
        CacheStore cache = new CacheStore(dir.resolve("cache"));
        String hash = sha1("PAYLOAD");
        Path tmp = dir.resolve("t.part");
        Files.writeString(tmp, "PAYLOAD");

        cache.ingest("assets/objects/" + hash.substring(0, 2) + "/" + hash, tmp);

        assertTrue(cache.contains("assets/objects/" + hash.substring(0, 2) + "/" + hash, hash));
        assertFalse(cache.contains("assets/objects/" + hash.substring(0, 2) + "/" + hash, sha1("OTHER")), "摘要不符不应命中");

        Path target = dir.resolve("game/assets/objects/x/y");
        cache.linkInto("assets/objects/" + hash.substring(0, 2) + "/" + hash, target);
        assertEquals("PAYLOAD", Files.readString(target));
    }

    @Test
    void secondGameDirSharesCacheWithoutNetwork() throws Exception {
        String content = "SHARED-ASSET";
        String hash = sha1(content);
        String url = "https://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash;
        StubFileDownloader stub = new StubFileDownloader().respond(url, content);
        CacheStore cache = new CacheStore(dir.resolve("cache"));

        Path gameA = dir.resolve("gameA");
        Path gameB = dir.resolve("gameB");
        FileEntry entryA = new FileEntry(List.of(url), gameA.resolve("assets/objects/" + hash.substring(0, 2) + "/" + hash), hash, content.length());
        FileEntry entryB = new FileEntry(List.of(url), gameB.resolve("assets/objects/" + hash.substring(0, 2) + "/" + hash), hash, content.length());

        var installer = new GameInstaller(stub, 2, cache, gameA);
        installer.install(new InstallPlan(List.of(entryA), gameA.resolve("versions/x/x.json"), "x"), writeJson(), listener());
        assertEquals(1, stub.hitCount(url));

        new GameInstaller(stub, 2, cache, gameB)
                .install(new InstallPlan(List.of(entryB), gameB.resolve("versions/x/x.json"), "x"), writeJson(), listener());

        assertEquals(1, stub.hitCount(url), "第二个游戏目录应命中缓存，零网络");
        assertEquals(content, Files.readString(entryB.target()));
    }

    private Path writeJson() throws Exception {
        Path json = dir.resolve("src.json");
        Files.writeString(json, "{}");
        return json;
    }

    private static GameInstaller.Listener listener() {
        return (entry, skipped) -> {};
    }
}
