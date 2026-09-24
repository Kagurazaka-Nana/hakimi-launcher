package com.minecraft.launcher.download.mojang;

import com.minecraft.launcher.model.manifest.VersionManifest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManifestServiceTest {

    private static final String MANIFEST_JSON = """
            {"latest":{"release":"1.21.1","snapshot":"24w21a"},
             "versions":[{"id":"1.21.1","type":"release",
               "url":"https://piston-meta.mojang.com/v1/packages/eb17/1.21.1.json",
               "time":"2024-08-08T12:24:45+00:00","releaseTime":"2024-08-08T12:24:45+00:00",
               "sha1":"eb17d0d5","complianceLevel":1}]}
            """;

    @TempDir
    Path dir;

    @Test
    void fetchParsesV2FieldsAndCaches() throws IOException {
        StubFileDownloader stub = new StubFileDownloader().respond(MojangProvider.VERSION_MANIFEST_V2, MANIFEST_JSON);
        Path cache = dir.resolve("manifest.json");
        ManifestService service = new ManifestService(new MojangProvider(), stub, cache, Duration.ofHours(1));

        VersionManifest manifest = service.fetch();

        assertEquals("1.21.1", manifest.getLatest().getRelease());
        var info = manifest.findVersionById("1.21.1").orElseThrow();
        assertEquals("eb17d0d5", info.getSha1());
        assertEquals(1, info.getComplianceLevel());
        assertTrue(Files.isRegularFile(cache), "应写入缓存文件");
        assertEquals(1, stub.hitCount(MojangProvider.VERSION_MANIFEST_V2));
    }

    @Test
    void ttlHitSkipsNetwork() throws IOException {
        StubFileDownloader stub = new StubFileDownloader().respond(MojangProvider.VERSION_MANIFEST_V2, MANIFEST_JSON);
        ManifestService service = new ManifestService(new MojangProvider(), stub, dir.resolve("manifest.json"), Duration.ofHours(1));

        service.fetch();
        service.fetch();

        assertEquals(1, stub.hitCount(MojangProvider.VERSION_MANIFEST_V2), "TTL 内不应重复下载");
    }

    @Test
    void fallsBackToStaleCacheWhenAllCandidatesFail() throws IOException {
        Path cache = dir.resolve("manifest.json");
        Files.writeString(cache, MANIFEST_JSON, StandardCharsets.UTF_8);
        Files.setLastModifiedTime(cache, FileTime.from(Instant.EPOCH));
        StubFileDownloader stub = new StubFileDownloader(); // 任何 URL 都失败
        ManifestService service = new ManifestService(new MojangProvider(), stub, cache, Duration.ofSeconds(1));

        VersionManifest manifest = service.fetch();

        assertEquals("1.21.1", manifest.getLatest().getRelease());
    }

    @Test
    void corruptCacheIsRefetched() throws IOException {
        Path cache = dir.resolve("manifest.json");
        Files.writeString(cache, "{not json", StandardCharsets.UTF_8);
        StubFileDownloader stub = new StubFileDownloader().respond(MojangProvider.VERSION_MANIFEST_V2, MANIFEST_JSON);
        ManifestService service = new ManifestService(new MojangProvider(), stub, cache, Duration.ofHours(1));

        VersionManifest manifest = service.fetch();

        assertEquals("1.21.1", manifest.getLatest().getRelease());
    }

    @Test
    void throwsWhenNoCacheAndAllFail() {
        StubFileDownloader stub = new StubFileDownloader();
        ManifestService service = new ManifestService(new MojangProvider(), stub, dir.resolve("missing.json"), Duration.ofHours(1));

        assertThrows(IOException.class, service::fetch);
    }
}
