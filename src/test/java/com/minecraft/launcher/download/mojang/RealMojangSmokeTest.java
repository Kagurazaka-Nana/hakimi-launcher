package com.minecraft.launcher.download.mojang;

import com.minecraft.launcher.download.BitFileDownloader;
import com.minecraft.launcher.download.DownloadConfig;
import com.minecraft.launcher.download.FileDownloader;
import com.minecraft.launcher.model.manifest.VersionInfo;
import com.minecraft.launcher.model.manifest.VersionManifest;
import com.minecraft.launcher.model.rule.RuleContext;
import com.minecraft.launcher.model.rule.RuleEvaluator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 真实端点冒烟测试（默认跳过）：设环境变量 HAKIMI_SMOKE=1 时联网验证
 * manifest → 1.21.1 解析 → 完整文件清单（只出计划，不落盘下载）。
 * 走本机开发代理 127.0.0.1:10808。
 */
class RealMojangSmokeTest {

    @TempDir
    Path dir;

    @Test
    void resolves1211PlanAgainstRealEndpoints() throws Exception {
        assumeTrue(System.getenv("HAKIMI_SMOKE") != null, "设置 HAKIMI_SMOKE=1 才联网冒烟");
        FileDownloader downloader = new BitFileDownloader(
                DownloadConfig.builder().proxyHost("127.0.0.1").proxyPort(10808).build());
        try {
            MojangProvider provider = new MojangProvider();
            ManifestService manifestService = new ManifestService(provider, downloader, dir.resolve("manifest.json"), Duration.ofHours(1));
            VersionManifest manifest = manifestService.fetch();
            VersionInfo info = manifest.findVersionById("1.21.1").orElseThrow();
            assertNotNull(info.getSha1(), "v2 清单应带 sha1");

            VersionJsonService versionJson = new VersionJsonService(provider, downloader, dir);
            ResolvedVersion resolved = versionJson.resolve(manifest, info);
            assertEquals("1.21.1", resolved.effectiveJarId());
            assertEquals("net.minecraft.client.main.Main", resolved.meta().getMainClass());

            LibraryFilter filter = new LibraryFilter(new RuleEvaluator(RuleContext.fromSystem()));
            var libs = filter.select(resolved.meta().getLibraries());
            var plan = new InstallPlanner(provider, new GameLayout(dir.resolve("mc"))).plan(resolved, libs);

            // 1.21.1 原版：客户端 JAR + 数十个库
            assertTrue(plan.files().size() > 30, "文件清单过小: " + plan.files().size());
            assertEquals("30c73b1c5da787909b2f73340419fdf13b9def88",
                    plan.files().get(0).sha1(), "client.jar 摘要应与实测值一致");
            if ("windows".equals(RuleContext.fromSystem().getOsName())) {
                // 现代版本（1.13+）natives 是 name 带 classifier 的独立构件；natives-map 机制服务老版本
                assertTrue(libs.stream().anyMatch(d -> d.name().endsWith(":natives-windows")),
                        "Windows 平台应包含 natives-windows 构件");
            }

            // P3：真实资源索引（1.21.1 → assets 17）下载 + sha1 校验 + 对象差集
            var assetService = new AssetService(provider, downloader, new GameLayout(dir.resolve("mc")));
            var index = assetService.loadIndex(resolved.meta().getAssetIndex());
            assertTrue(index.getObjects().size() > 1000, "assets 17 应有上千对象: " + index.getObjects().size());
            var objectPlan = assetService.planObjects(index, false);
            assertEquals(index.getObjects().size(), objectPlan.size(), "空目录应全量入计划");
            assertTrue(objectPlan.get(0).candidateUrls().get(0).startsWith("https://resources.download.minecraft.net/"));

            // P4：真实 Java runtime 目录 → java-runtime-delta 平台清单计划
            var runtimeService = new RuntimeService(provider, downloader,
                    new GameLayout(dir.resolve("mc")), dir.resolve("runtime-cache"));
            var runtimePlan = runtimeService.plan("java-runtime-delta",
                    RuleContext.fromSystem().getOsName(), RuleContext.fromSystem().getArch());
            assertTrue(runtimePlan.files().size() > 50, "JRE 文件数过少: " + runtimePlan.files().size());
            assertTrue(runtimePlan.files().get(0).candidateUrls().get(0).startsWith("https://piston-data.mojang.com/"));
        } finally {
            downloader.close();
        }
    }
}
