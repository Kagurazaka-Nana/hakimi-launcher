package com.minecraft.launcher.download.mojang;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AutoProviderTest {

    private final AutoProvider auto = new AutoProvider(List.of(new BmclApiProvider(), new MojangProvider()));

    @Test
    void mergesCandidatesInOrderDeduped() {
        List<String> urls = auto.getVersionListUrls();
        assertEquals(2, urls.size());
        assertEquals("https://bmclapi2.bangbang93.com/mc/game/version_manifest_v2.json", urls.get(0));
        assertEquals("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json", urls.get(1));
    }

    @Test
    void deduplicatesIdenticalCandidates() {
        AutoProvider sameTwice = new AutoProvider(List.of(new MojangProvider(), new MojangProvider()));
        assertEquals(1, sameTwice.getVersionListUrls().size());
    }

    @Test
    void injectsBothMirrorsForOriginalUrls() {
        List<String> urls = auto.injectURLCandidates("https://piston-data.mojang.com/v1/objects/30c7/client.jar");
        assertEquals(List.of(
                "https://bmclapi2.bangbang93.com/v1/objects/30c7/client.jar",
                "https://piston-data.mojang.com/v1/objects/30c7/client.jar"), urls);
    }

    @Test
    void concurrencyTakesMinimum() {
        assertEquals(8, auto.getMaxConcurrency());
    }

    @Test
    void emptyProvidersRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AutoProvider(List.of()));
    }
}
