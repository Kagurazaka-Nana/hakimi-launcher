package com.minecraft.launcher.download.mojang;

import com.minecraft.launcher.model.rule.RuleContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 端到端离线安装测试：假 manifest → 版本 JSON → 客户端 JAR + library + asset 对象 + runtime 文件
 * 全链路落盘、阶段进度、提交标记与二次安装零网络。
 */
class InstallationServiceTest {

    @TempDir
    Path dir;

    // SHA-1 为 Mojang 协议内容寻址标识（完整性语义，非安全用途）
    private static String sha1(String content) throws Exception {
        MessageDigest md = MessageDigest.getInstance(Checksums.MOJANG_DIGEST);
        return HexFormat.of().formatHex(md.digest(content.getBytes(StandardCharsets.UTF_8)));
    }

    private static final class TestProvider implements DownloadProvider {
        @Override
        public List<String> getVersionListUrls() {
            return List.of("https://x.test/manifest.json");
        }

        @Override
        public List<String> getAssetObjectCandidates(String location) {
            return List.of("https://x.test/objects/" + location);
        }

        @Override
        public List<String> injectURLCandidates(String baseURL) {
            return List.of(baseURL);
        }

        @Override
        public List<String> getJavaRuntimeCatalogUrls() {
            return List.of("https://x.test/catalog.json");
        }

        @Override
        public int getMaxConcurrency() {
            return 4;
        }
    }

    @Test
    void fullInstallLandsAllArtifactsAndIsIdempotent() throws Exception {
        String platform = RuntimeService.platformKey(RuleContext.fromSystem().getOsName(), RuleContext.fromSystem().getArch());

        String clientJar = "CLIENTJAR";
        String libJar = "LIBJAR!";
        String obj = "OBJ1!";
        String rfile = "RF!!";

        String assetIndexJson = """
                {"objects":{"a/b.txt":{"hash":"%s","size":%d}}}
                """.formatted(sha1(obj), obj.length());
        String versionJson = """
                {"id":"9.9.9","type":"release","mainClass":"M","assets":"99",
                 "assetIndex":{"id":"99","sha1":"%s","size":%d,"totalSize":%d,"url":"https://x.test/ai.json"},
                 "downloads":{"client":{"url":"https://x.test/client.jar","sha1":"%s","size":%d}},
                 "javaVersion":{"component":"java-runtime-test","majorVersion":21},
                 "libraries":[{"name":"org:test:1","downloads":{"artifact":{"path":"org/test/1/test-1.jar","sha1":"%s","size":%d,"url":"https://x.test/lib.jar"}}}]}
                """.formatted(sha1(assetIndexJson), assetIndexJson.length(), obj.length(),
                sha1(clientJar), clientJar.length(), sha1(libJar), libJar.length());
        String runtimeManifestJson = """
                {"files":{"bin/x":{"type":"file","downloads":{"raw":{"url":"https://x.test/rfile","sha1":"%s","size":%d}}}}}
                """.formatted(sha1(rfile), rfile.length());
        String catalogJson = """
                {"%s":{"java-runtime-test":[{"manifest":{"sha1":"%s","size":%d,"url":"https://x.test/rm.json"},"version":{"name":"1","released":"t"}}]}}
                """.formatted(platform, sha1(runtimeManifestJson), runtimeManifestJson.length());
        String manifestJson = """
                {"latest":{"release":"9.9.9","snapshot":"9.9.9"},
                 "versions":[{"id":"9.9.9","type":"release","url":"https://x.test/v.json","time":"t","releaseTime":"t","sha1":"s","complianceLevel":1}]}
                """;

        StubFileDownloader stub = new StubFileDownloader()
                .respond("https://x.test/manifest.json", manifestJson)
                .respond("https://x.test/v.json", versionJson)
                .respond("https://x.test/client.jar", clientJar)
                .respond("https://x.test/lib.jar", libJar)
                .respond("https://x.test/ai.json", assetIndexJson)
                .respond("https://x.test/objects/" + sha1(obj).substring(0, 2) + "/" + sha1(obj), obj)
                .respond("https://x.test/catalog.json", catalogJson)
                .respond("https://x.test/rm.json", runtimeManifestJson)
                .respond("https://x.test/rfile", rfile);

        TestProvider provider = new TestProvider();
        Path gameDir = dir.resolve("game");
        Path cacheDir = dir.resolve("cache");
        GameLayout layout = new GameLayout(gameDir);
        InstallationService service = new InstallationService(
                new ManifestService(provider, stub, cacheDir.resolve("manifest.json"), java.time.Duration.ofHours(1)),
                new VersionJsonService(provider, stub, cacheDir.resolve("version-jsons")),
                provider, stub, layout, cacheDir);

        assertFalse(service.isInstalled("9.9.9"));
        List<String> stages = new ArrayList<>();
        service.install("9.9.9", p -> {
            if (stages.isEmpty() || !stages.get(stages.size() - 1).equals(p.stage())) {
                stages.add(p.stage());
            }
        });

        // 全部工件落盘
        assertEquals(clientJar, Files.readString(layout.versionJar("9.9.9")));
        assertEquals(libJar, Files.readString(layout.library("org/test/1/test-1.jar")));
        assertEquals(obj, Files.readString(layout.assetObject(sha1(obj).substring(0, 2) + "/" + sha1(obj))));
        assertEquals(rfile, Files.readString(layout.runtime("java-runtime-test", platform).resolve("bin/x")));
        assertTrue(service.isInstalled("9.9.9"), "版本 JSON 应作为提交标记最后写入");
        assertEquals(List.of("game", "assets", "runtime"), stages);

        // 二次安装：全部命中跳过/缓存，零新增网络
        int hitsBefore = stub.totalHits();
        service.install("9.9.9", p -> {});
        assertEquals(hitsBefore, stub.totalHits(), "二次安装不应有任何网络请求");
    }
}
