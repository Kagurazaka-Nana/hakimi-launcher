package com.minecraft.launcher.download.mojang;

import com.minecraft.launcher.model.manifest.VersionInfo;
import com.minecraft.launcher.model.manifest.VersionManifest;
import com.minecraft.launcher.util.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VersionJsonServiceTest {

    private static final String PARENT_URL = "https://piston-meta.mojang.com/v1/packages/eb17/1.21.1.json";
    private static final String CHILD_URL = "https://piston-meta.mojang.com/v1/packages/fa01/1.21.1-fabric.json";

    private static final String PARENT_JSON = """
            {"id":"1.21.1","type":"release",
             "mainClass":"net.minecraft.client.main.Main","assets":"17",
             "assetIndex":{"id":"17","sha1":"de57","size":449557,"totalSize":824951171,
               "url":"https://piston-meta.mojang.com/v1/packages/de57/17.json"},
             "downloads":{"client":{"url":"https://piston-data.mojang.com/v1/objects/30c7/client.jar","sha1":"30c7","size":26836906}},
             "javaVersion":{"component":"java-runtime-delta","majorVersion":21},
             "libraries":[{"name":"com.mojang:logging:1.1.1"},{"name":"org.lwjgl:lwjgl:3.3.3"}],
             "logging":{"client":{"argument":"-Dlog4j.configurationFile=${path}",
               "file":{"id":"client-1.12.xml","sha1":"bd65","size":618,
                 "url":"https://piston-data.mojang.com/v1/objects/bd65/client-1.12.xml","type":"log4j2-xml"}}},
             "minimumLauncherVersion":21,"complianceLevel":1}
            """;

    private static final String CHILD_JSON = """
            {"id":"1.21.1-fabric","type":"release","inheritsFrom":"1.21.1","jar":"1.21.1",
             "mainClass":"net.fabricmc.loader.impl.launch.knot.KnotClient",
             "libraries":[{"name":"net.fabricmc:fabric-loader:0.16.0"},{"name":"com.mojang:logging:1.2.0"}]}
            """;

    @TempDir
    Path dir;

    private VersionManifest manifest() throws IOException {
        return JsonUtils.fromJson("""
                {"latest":{"release":"1.21.1","snapshot":"1.21.1"},
                 "versions":[
                   {"id":"1.21.1","type":"release","url":"%s","time":"t","releaseTime":"t","sha1":"eb17","complianceLevel":1},
                   {"id":"1.21.1-fabric","type":"release","url":"%s","time":"t","releaseTime":"t","sha1":"fa01","complianceLevel":1}
                 ]}
                """.formatted(PARENT_URL, CHILD_URL), VersionManifest.class);
    }

    private VersionInfo entry(String id) throws IOException {
        return manifest().findVersionById(id).orElseThrow();
    }

    @Test
    void parsesInheritanceFields() throws IOException {
        StubFileDownloader stub = new StubFileDownloader()
                .respond(CHILD_URL, CHILD_JSON)
                .respond(PARENT_URL, PARENT_JSON);
        VersionJsonService service = new VersionJsonService(new MojangProvider(), stub, dir);

        var child = service.fetch(entry("1.21.1-fabric"));

        assertEquals("1.21.1", child.getInheritsFrom());
        assertEquals("1.21.1", child.getJar());
    }

    @Test
    void resolveMergesChildOverParent() throws IOException {
        StubFileDownloader stub = new StubFileDownloader()
                .respond(CHILD_URL, CHILD_JSON)
                .respond(PARENT_URL, PARENT_JSON);
        VersionJsonService service = new VersionJsonService(new MojangProvider(), stub, dir);

        ResolvedVersion resolved = service.resolve(manifest(), entry("1.21.1-fabric"));

        assertEquals(List.of("1.21.1-fabric", "1.21.1"), resolved.chain());
        assertEquals("1.21.1", resolved.effectiveJarId());
        var meta = resolved.meta();
        assertEquals("1.21.1-fabric", meta.getClientVersion());
        assertEquals("net.fabricmc.loader.impl.launch.knot.KnotClient", meta.getMainClass()); // 子覆盖
        assertEquals("net.minecraft.client.main.Main", service.resolve(manifest(), entry("1.21.1")).meta().getMainClass()); // 父自身保留
        // 父补齐
        assertEquals("17", meta.getAssets());
        assertEquals("de57", meta.getAssetIndex().getSha1());
        assertEquals("30c7", meta.getDownloads().getClient().getSha1());
        assertEquals("java-runtime-delta", meta.getJavaVersion().getComponent());
        assertEquals("client-1.12.xml", meta.getLogging().getClient().getFile().getId());
        assertNull(meta.getInheritsFrom());
        // libraries：子在前、按 name 去重（保留子版本 1.2.0，丢弃父 1.1.1）
        assertEquals(
                List.of("net.fabricmc:fabric-loader:0.16.0", "com.mojang:logging:1.2.0", "org.lwjgl:lwjgl:3.3.3"),
                meta.getLibraries().stream().map(l -> l.getName()).toList());
    }

    @Test
    void resolveIsCachedAndIdempotent() throws IOException {
        StubFileDownloader stub = new StubFileDownloader()
                .respond(CHILD_URL, CHILD_JSON)
                .respond(PARENT_URL, PARENT_JSON);
        VersionJsonService service = new VersionJsonService(new MojangProvider(), stub, dir);

        service.resolve(manifest(), entry("1.21.1-fabric"));
        service.resolve(manifest(), entry("1.21.1-fabric"));

        assertEquals(1, stub.hitCount(CHILD_URL));
        assertEquals(1, stub.hitCount(PARENT_URL));
    }

    @Test
    void missingJarFieldResolvesToRootId() throws IOException {
        String childNoJar = CHILD_JSON.replace(",\"jar\":\"1.21.1\"", "");
        StubFileDownloader stub = new StubFileDownloader()
                .respond(CHILD_URL, childNoJar)
                .respond(PARENT_URL, PARENT_JSON);
        VersionJsonService service = new VersionJsonService(new MojangProvider(), stub, dir);

        ResolvedVersion resolved = service.resolve(manifest(), entry("1.21.1-fabric"));

        assertEquals("1.21.1", resolved.effectiveJarId());
    }

    @Test
    void inheritsFromCycleIsRejected() throws IOException {
        String loopJson = CHILD_JSON.replace("\"inheritsFrom\":\"1.21.1\"", "\"inheritsFrom\":\"1.21.1-fabric\"");
        StubFileDownloader stub = new StubFileDownloader()
                .respond(CHILD_URL, loopJson)
                .respond(PARENT_URL, PARENT_JSON);
        VersionJsonService service = new VersionJsonService(new MojangProvider(), stub, dir);

        assertThrows(IllegalStateException.class, () -> service.resolve(manifest(), entry("1.21.1-fabric")));
    }

    @Test
    void unknownParentFailsWithIoException() throws IOException {
        String badParent = CHILD_JSON.replace("\"inheritsFrom\":\"1.21.1\"", "\"inheritsFrom\":\"9.9.9\"");
        StubFileDownloader stub = new StubFileDownloader().respond(CHILD_URL, badParent);
        VersionJsonService service = new VersionJsonService(new MojangProvider(), stub, dir);

        assertThrows(IOException.class, () -> service.resolve(manifest(), entry("1.21.1-fabric")));
    }
}
