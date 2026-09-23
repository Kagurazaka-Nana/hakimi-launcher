package com.minecraft.launcher.download.mojang;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BmclApiProviderTest {

    private final BmclApiProvider provider = new BmclApiProvider();

    @Test
    void rewritesAllMojangDomains() {
        assertEquals("https://bmclapi2.bangbang93.com/mc/game/version_manifest_v2.json",
                BmclApiProvider.rewrite("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"));
        assertEquals("https://bmclapi2.bangbang93.com/v1/packages/eb17/1.21.1.json",
                BmclApiProvider.rewrite("https://piston-meta.mojang.com/v1/packages/eb17/1.21.1.json"));
        assertEquals("https://bmclapi2.bangbang93.com/v1/objects/30c7/client.jar",
                BmclApiProvider.rewrite("https://piston-data.mojang.com/v1/objects/30c7/client.jar"));
        assertEquals("https://bmclapi2.bangbang93.com/maven/com/mojang/logging/1.1.1/logging-1.1.1.jar",
                BmclApiProvider.rewrite("https://libraries.minecraft.net/com/mojang/logging/1.1.1/logging-1.1.1.jar"));
        assertEquals("https://bmclapi2.bangbang93.com/objects/de/de57",
                BmclApiProvider.rewrite("https://resources.download.minecraft.net/de/de57"));
        assertEquals("https://bmclapi2.bangbang93.com/v1/products/java-runtime/x/all.json",
                BmclApiProvider.rewrite("https://launchermeta.mojang.com/v1/products/java-runtime/x/all.json"));
    }

    @Test
    void unknownUrlPassesThroughUnchanged() {
        String url = "https://maven.minecraftforge.net/net/minecraftforge/forge/x/forge.jar";
        assertEquals(url, BmclApiProvider.rewrite(url));
    }

    @Test
    void providerEndpointsUseMirror() {
        assertEquals(List.of("https://bmclapi2.bangbang93.com/mc/game/version_manifest_v2.json"),
                provider.getVersionListUrls());
        assertEquals(List.of("https://bmclapi2.bangbang93.com/objects/aa/aabb"),
                provider.getAssetObjectCandidates("aa/aabb"));
        assertEquals(8, provider.getMaxConcurrency());
    }
}
