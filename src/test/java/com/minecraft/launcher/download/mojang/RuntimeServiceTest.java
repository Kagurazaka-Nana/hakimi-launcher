package com.minecraft.launcher.download.mojang;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeServiceTest {

    @TempDir
    Path dir;

    // SHA-1 为 Mojang 协议内容寻址标识（完整性语义，非安全用途）
    private static String sha1(String content) throws Exception {
        MessageDigest md = MessageDigest.getInstance(Checksums.MOJANG_DIGEST);
        return HexFormat.of().formatHex(md.digest(content.getBytes(StandardCharsets.UTF_8)));
    }

    private static final String MANIFEST_JSON = """
            {"files":{
              "bin/java.exe":{"type":"file","executable":true,
                "downloads":{"raw":{"url":"https://x.test/java.exe","sha1":"rawhash","size":9},
                             "lzma":{"url":"https://x.test/java.exe.lzma","sha1":"lz","size":5}}},
              "lib":{"type":"directory"},
              "conf/jvm.cfg":{"type":"file",
                "downloads":{"raw":{"url":"https://x.test/jvm.cfg","sha1":"cfghash","size":1}}}
            }}
            """;

    private StubFileDownloader stub() throws Exception {
        return new StubFileDownloader()
                .respond("https://x.test/catalog.json", """
                        {"windows-x64":{"java-runtime-delta":[{
                          "manifest":{"sha1":"%s","size":%d,"url":"https://x.test/manifest.json"},
                          "version":{"name":"21.0.7","released":"2025-05-19T08:34:42+00:00"}}]}}
                        """.formatted(sha1(MANIFEST_JSON), MANIFEST_JSON.length()))
                .respond("https://x.test/manifest.json", MANIFEST_JSON);
    }

    private RuntimeService service(StubFileDownloader stub) {
        DownloadProvider provider = new DownloadProvider() {
            @Override
            public java.util.List<String> getVersionListUrls() {
                return java.util.List.of();
            }

            @Override
            public java.util.List<String> getAssetObjectCandidates(String assetObjectLocation) {
                return java.util.List.of();
            }

            @Override
            public java.util.List<String> injectURLCandidates(String baseURL) {
                return java.util.List.of(baseURL);
            }

            @Override
            public java.util.List<String> getJavaRuntimeCatalogUrls() {
                return java.util.List.of("https://x.test/catalog.json");
            }

            @Override
            public int getMaxConcurrency() {
                return 8;
            }
        };
        return new RuntimeService(provider, stub, new GameLayout(dir.resolve("game")), dir.resolve("cache"));
    }

    @Test
    void platformKeyMapping() {
        assertEquals("windows-x64", RuntimeService.platformKey("windows", "x64"));
        assertEquals("windows-x86", RuntimeService.platformKey("windows", "x86"));
        assertEquals("windows-arm64", RuntimeService.platformKey("windows", "arm64"));
        assertEquals("mac-os-arm64", RuntimeService.platformKey("osx", "arm64"));
        assertEquals("mac-os", RuntimeService.platformKey("osx", "x64"));
        assertEquals("linux", RuntimeService.platformKey("linux", "x64"));
        assertEquals("linux-i386", RuntimeService.platformKey("linux", "x86"));
        assertThrows(IllegalArgumentException.class, () -> RuntimeService.platformKey("solaris", "x64"));
    }

    @Test
    void plansRawFilesAndSkipsDirectories() throws Exception {
        StubFileDownloader stub = stub();
        RuntimeService service = service(stub);

        RuntimeService.RuntimePlan plan = service.plan("java-runtime-delta", "windows", "x64");

        assertEquals(2, plan.files().size(), "directory 条目不应入计划");
        assertEquals(dir.resolve("game/runtime/java-runtime-delta/windows-x64"), plan.runtimeDir());
        assertEquals(java.util.List.of("bin/java.exe"), plan.executablePaths());
        FileEntry javaExe = plan.files().stream().filter(f -> f.target().endsWith("java.exe")).findFirst().orElseThrow();
        assertEquals(java.util.List.of("https://x.test/java.exe"), javaExe.candidateUrls(), "只取 raw，忽略 lzma");
        assertEquals("rawhash", javaExe.sha1());
    }

    @Test
    void catalogAndManifestAreCached() throws Exception {
        StubFileDownloader stub = stub();
        RuntimeService service = service(stub);

        service.plan("java-runtime-delta", "windows", "x64");
        service.plan("java-runtime-delta", "windows", "x64");

        assertEquals(1, stub.hitCount("https://x.test/catalog.json"));
        assertEquals(1, stub.hitCount("https://x.test/manifest.json"));
    }

    @Test
    void unknownComponentThrows() throws Exception {
        RuntimeService service = service(stub());
        assertThrows(IOException.class, () -> service.plan("java-runtime-zeta", "windows", "x64"));
    }

    @Test
    void manifestShaMismatchThrows() throws Exception {
        StubFileDownloader stub = new StubFileDownloader()
                .respond("https://x.test/catalog.json", """
                        {"windows-x64":{"java-runtime-delta":[{
                          "manifest":{"sha1":"deadbeef","size":1,"url":"https://x.test/manifest.json"},
                          "version":{"name":"21","released":"t"}}]}}
                        """)
                .respond("https://x.test/manifest.json", MANIFEST_JSON);
        RuntimeService service = service(stub);

        assertThrows(IOException.class, () -> service.plan("java-runtime-delta", "windows", "x64"));
    }

    @Test
    void applyExecutableBitsIsSafeNoopOnWindows() throws Exception {
        StubFileDownloader stub = stub();
        RuntimeService service = service(stub);
        RuntimeService.RuntimePlan plan = service.plan("java-runtime-delta", "windows", "x64");
        Files.createDirectories(plan.runtimeDir().resolve("bin"));
        Files.writeString(plan.runtimeDir().resolve("bin/java.exe"), "x");

        service.applyExecutableBits(plan); // 不应抛异常（Windows 无 posix 视图）

        assertTrue(Files.isRegularFile(plan.runtimeDir().resolve("bin/java.exe")));
    }
}
