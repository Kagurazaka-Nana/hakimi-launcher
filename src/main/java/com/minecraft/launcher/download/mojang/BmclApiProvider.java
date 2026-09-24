package com.minecraft.launcher.download.mojang;

import java.util.List;
import java.util.Objects;

/**
 * BMCLAPI 国内镜像源（bangbang93）：对 Mojang 各域做前缀改写。
 * 映射依据 2026-09-24 实测 + BMCLV2 源码：piston-meta→/mc/game、/v1/packages；
 * piston-data→/v1/objects；launchermeta products→/v1/products；
 * libraries→/maven；resources→/objects（该路径可能仅 302/404，由候选回退兜底）。
 */
public final class BmclApiProvider implements DownloadProvider {

    public static final String BASE = "https://bmclapi2.bangbang93.com/";

    /** 官方 URL → BMCLAPI 等价 URL；未匹配的前缀原样返回。 */
    public static String rewrite(String url) {
        Objects.requireNonNull(url, "url");
        return url
                .replace("https://piston-meta.mojang.com/mc/game/", BASE + "mc/game/")
                .replace("https://launchermeta.mojang.com/mc/game/", BASE + "mc/game/")
                .replace("https://piston-meta.mojang.com/v1/packages/", BASE + "v1/packages/")
                .replace("https://launchermeta.mojang.com/v1/packages/", BASE + "v1/packages/")
                .replace("https://launchermeta.mojang.com/v1/products/", BASE + "v1/products/")
                .replace("https://piston-data.mojang.com/v1/objects/", BASE + "v1/objects/")
                .replace("https://libraries.minecraft.net/", BASE + "maven/")
                .replace("https://resources.download.minecraft.net/", BASE + "objects/");
    }

    @Override
    public List<String> getVersionListUrls() {
        return List.of(BASE + "mc/game/version_manifest_v2.json");
    }

    @Override
    public List<String> getAssetObjectCandidates(String assetObjectLocation) {
        return List.of(BASE + "objects/" + assetObjectLocation);
    }

    @Override
    public List<String> injectURLCandidates(String baseURL) {
        return List.of(rewrite(baseURL));
    }

    @Override
    public List<String> getJavaRuntimeCatalogUrls() {
        return List.of(rewrite(MojangProvider.JAVA_RUNTIME_CATALOG));
    }

    @Override
    public int getMaxConcurrency() {
        return 8;
    }
}
