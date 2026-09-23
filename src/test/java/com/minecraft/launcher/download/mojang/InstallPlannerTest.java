package com.minecraft.launcher.download.mojang;

import com.minecraft.launcher.model.version.VersionMeta;
import com.minecraft.launcher.model.version.downloads.DownloadType;
import com.minecraft.launcher.model.version.downloads.Downloads;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InstallPlannerTest {

    @TempDir
    Path dir;

    @Test
    void plansClientJarAndLibrariesIntoLayout() {
        VersionMeta meta = VersionMeta.builder()
                .clientVersion("1.21.1-fabric")
                .downloads(new Downloads(new DownloadType("30c7", 26836906, "https://piston-data.mojang.com/v1/objects/30c7/client.jar"), null))
                .build();
        ResolvedVersion resolved = new ResolvedVersion(meta, "1.21.1", List.of("1.21.1-fabric", "1.21.1"), dir.resolve("cache/1.21.1-fabric.json"));
        GameLayout layout = new GameLayout(dir);
        InstallPlanner planner = new InstallPlanner(new MojangProvider(), layout);

        InstallPlan plan = planner.plan(resolved, List.of(
                new LibraryDownload("com.mojang:logging:1.1.1", "com/mojang/logging/1.1.1/logging-1.1.1.jar",
                        "https://libraries.minecraft.net/com/mojang/logging/1.1.1/logging-1.1.1.jar", "aaa", 100, false, List.of())));

        assertEquals("1.21.1", plan.effectiveJarId());
        assertEquals(dir.resolve("versions/1.21.1-fabric/1.21.1-fabric.json"), plan.versionJsonTarget());
        assertEquals(2, plan.files().size());
        // 客户端 JAR 落在 effectiveJarId 目录，而不是加载器 id 目录
        assertEquals(dir.resolve("versions/1.21.1/1.21.1.jar"), plan.files().get(0).target());
        assertEquals("30c7", plan.files().get(0).sha1());
        assertEquals(26836906, plan.files().get(0).size());
        assertEquals(dir.resolve("libraries/com/mojang/logging/1.1.1/logging-1.1.1.jar"), plan.files().get(1).target());
        assertEquals(List.of("https://piston-data.mojang.com/v1/objects/30c7/client.jar"), plan.files().get(0).candidateUrls());
    }
}
