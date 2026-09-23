package com.minecraft.launcher.download.mojang;

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

class GameInstallerTest {

    @TempDir
    Path dir;

    private static String sha1(String content) throws Exception {
        MessageDigest md = MessageDigest.getInstance(Checksums.MOJANG_DIGEST);
        return HexFormat.of().formatHex(md.digest(content.getBytes(StandardCharsets.UTF_8)));
    }

    private FileEntry entry(String url, String targetRel, String content) throws Exception {
        return new FileEntry(List.of(url), dir.resolve(targetRel), sha1(content), content.length());
    }

    private InstallPlan planOf(List<FileEntry> files) {
        return new InstallPlan(files, dir.resolve("versions/1.21.1/1.21.1.json"), "1.21.1");
    }

    @Test
    void installsAllFilesAndCopiesVersionJson() throws Exception {
        StubFileDownloader stub = new StubFileDownloader()
                .respond("https://x.test/client.jar", "CLIENTJAR")
                .respond("https://x.test/lib.jar", "LIBJAR");
        Path jsonSource = dir.resolve("cache/1.21.1.json");
        Files.createDirectories(jsonSource.getParent());
        Files.writeString(jsonSource, "{\"id\":\"1.21.1\"}");

        var installer = new GameInstaller(stub, 4);
        GameInstaller.Result result = installer.install(
                planOf(List.of(
                        entry("https://x.test/client.jar", "versions/1.21.1/1.21.1.jar", "CLIENTJAR"),
                        entry("https://x.test/lib.jar", "libraries/a/b/b.jar", "LIBJAR"))),
                jsonSource, listener());

        assertEquals(2, result.downloaded());
        assertEquals(0, result.skipped());
        assertEquals("CLIENTJAR", Files.readString(dir.resolve("versions/1.21.1/1.21.1.jar")));
        assertEquals("LIBJAR", Files.readString(dir.resolve("libraries/a/b/b.jar")));
        assertEquals("{\"id\":\"1.21.1\"}", Files.readString(dir.resolve("versions/1.21.1/1.21.1.json")));
    }

    @Test
    void secondInstallSkipsByChecksum() throws Exception {
        StubFileDownloader stub = new StubFileDownloader()
                .respond("https://x.test/client.jar", "CLIENTJAR")
                .respond("https://x.test/lib.jar", "LIBJAR");
        Path jsonSource = dir.resolve("cache/1.21.1.json");
        Files.createDirectories(jsonSource.getParent());
        Files.writeString(jsonSource, "{}");
        FileEntry client = entry("https://x.test/client.jar", "versions/1.21.1/1.21.1.jar", "CLIENTJAR");
        FileEntry lib = entry("https://x.test/lib.jar", "libraries/a/b/b.jar", "LIBJAR");

        var installer = new GameInstaller(stub, 4);
        installer.install(planOf(List.of(client, lib)), jsonSource, listener());
        GameInstaller.Result second = installer.install(planOf(List.of(client, lib)), jsonSource, listener());

        assertEquals(0, second.downloaded());
        assertEquals(2, second.skipped());
        assertEquals(1, stub.hitCount("https://x.test/client.jar"), "幂等：不应重复下载");
    }

    @Test
    void shaMismatchFallsBackToNextCandidate() throws Exception {
        StubFileDownloader stub = new StubFileDownloader()
                .respond("https://bad.test/client.jar", "CORRUPT")
                .respond("https://good.test/client.jar", "CLIENTJAR");
        Path jsonSource = dir.resolve("cache/1.21.1.json");
        Files.createDirectories(jsonSource.getParent());
        Files.writeString(jsonSource, "{}");
        FileEntry entry = new FileEntry(
                List.of("https://bad.test/client.jar", "https://good.test/client.jar"),
                dir.resolve("versions/1.21.1/1.21.1.jar"), sha1("CLIENTJAR"), 9);

        new GameInstaller(stub, 2).install(planOf(List.of(entry)), jsonSource, listener());

        assertEquals("CLIENTJAR", Files.readString(dir.resolve("versions/1.21.1/1.21.1.jar")));
        assertEquals(1, stub.hitCount("https://bad.test/client.jar"));
        assertEquals(1, stub.hitCount("https://good.test/client.jar"));
        try (var s = Files.list(dir.resolve("versions/1.21.1"))) {
            assertTrue(s.noneMatch(p -> p.getFileName().toString().endsWith(".part")), "失败候选的临时文件应被清理");
        }
    }

    @Test
    void allCandidatesFailingThrows() throws Exception {
        StubFileDownloader stub = new StubFileDownloader().respond("https://bad.test/x.jar", "WRONG");
        Path jsonSource = dir.resolve("cache/1.21.1.json");
        Files.createDirectories(jsonSource.getParent());
        Files.writeString(jsonSource, "{}");
        FileEntry entry = new FileEntry(List.of("https://bad.test/x.jar"), dir.resolve("libraries/x/x.jar"), sha1("RIGHT"), 5);

        var installer = new GameInstaller(stub, 2);
        assertThrows(IOException.class, () -> installer.install(planOf(List.of(entry)), jsonSource, listener()));
    }

    @Test
    void corruptExistingFileIsRedownloaded() throws Exception {
        Path target = dir.resolve("libraries/a/b/b.jar");
        Files.createDirectories(target.getParent());
        Files.writeString(target, "STALE-CORRUPT");
        StubFileDownloader stub = new StubFileDownloader().respond("https://x.test/lib.jar", "LIBJAR");
        Path jsonSource = dir.resolve("cache/1.21.1.json");
        Files.createDirectories(jsonSource.getParent());
        Files.writeString(jsonSource, "{}");

        GameInstaller.Result result = new GameInstaller(stub, 2).install(
                planOf(List.of(entry("https://x.test/lib.jar", "libraries/a/b/b.jar", "LIBJAR"))), jsonSource, listener());

        assertEquals(1, result.downloaded());
        assertEquals("LIBJAR", Files.readString(target));
    }

    private static GameInstaller.Listener listener() {
        return (entry, skipped) -> {};
    }
}
