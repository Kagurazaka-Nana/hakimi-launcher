package com.minecraft.launcher.download.mojang;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MojangProviderTest {

    private final MojangProvider provider = new MojangProvider();

    @Test
    void versionListPointsToOfficialV2Manifest() {
        List<String> urls = provider.getVersionListUrls();
        assertEquals(List.of("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"), urls);
    }

    @Test
    void assetObjectCandidatesUseOfficialCdn() {
        List<String> urls = provider.getAssetObjectCandidates("aa/aab3c4");
        assertEquals(List.of("https://resources.download.minecraft.net/aa/aab3c4"), urls);
    }

    @Test
    void injectIsIdentityForOfficialSource() {
        String url = "https://piston-meta.mojang.com/v1/packages/eb17d0/1.21.1.json";
        assertEquals(List.of(url), provider.injectURLCandidates(url));
    }

    @Test
    void concurrencyIsPositive() {
        assertTrue(provider.getMaxConcurrency() > 0);
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> provider.getAssetObjectCandidates(null));
        assertThrows(NullPointerException.class, () -> provider.injectURLCandidates(null));
    }
}
