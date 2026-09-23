package com.minecraft.launcher.download.mojang;

import java.util.List;
import java.util.Objects;

/**
 * Mojang 官方源（2026-09-23 实测端点，见 docs/DownloadPipeline.md §1）。
 * 官方 JSON 里的 URL 本身就是官方地址，因此 inject 为恒等映射。
 */
public final class MojangProvider implements DownloadProvider {

    public static final String VERSION_MANIFEST_V2 = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    public static final String ASSET_OBJECTS_BASE = "https://resources.download.minecraft.net/";
    public static final String LIBRARIES_BASE = "https://libraries.minecraft.net/";
    public static final String JAVA_RUNTIME_CATALOG =
            "https://launchermeta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json";

    private static final List<String> VERSION_LIST = List.of(VERSION_MANIFEST_V2);

    @Override
    public List<String> getVersionListUrls() {
        return VERSION_LIST;
    }

    @Override
    public List<String> getAssetObjectCandidates(String assetObjectLocation) {
        Objects.requireNonNull(assetObjectLocation, "assetObjectLocation");
        return List.of(ASSET_OBJECTS_BASE + assetObjectLocation);
    }

    @Override
    public List<String> injectURLCandidates(String baseURL) {
        Objects.requireNonNull(baseURL, "baseURL");
        return List.of(baseURL);
    }

    @Override
    public int getMaxConcurrency() {
        return 16;
    }
}
