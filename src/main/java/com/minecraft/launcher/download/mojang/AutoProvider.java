package com.minecraft.launcher.download.mojang;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 自动源：按给定顺序合并多个 Provider 的候选 URL（去重、保序），
 * 安装器按候选顺序回退——镜像挂了自动落到下一个（通常是官方源）。
 */
public final class AutoProvider implements DownloadProvider {

    private final List<DownloadProvider> ordered;

    public AutoProvider(List<DownloadProvider> ordered) {
        if (ordered.isEmpty()) {
            throw new IllegalArgumentException("AutoProvider 至少需要一个源");
        }
        this.ordered = List.copyOf(ordered);
    }

    @Override
    public List<String> getVersionListUrls() {
        return merge(p -> p.getVersionListUrls());
    }

    @Override
    public List<String> getAssetObjectCandidates(String assetObjectLocation) {
        return merge(p -> p.getAssetObjectCandidates(assetObjectLocation));
    }

    @Override
    public List<String> injectURLCandidates(String baseURL) {
        return merge(p -> p.injectURLCandidates(baseURL));
    }

    @Override
    public List<String> getJavaRuntimeCatalogUrls() {
        return merge(p -> p.getJavaRuntimeCatalogUrls());
    }

    @Override
    public int getMaxConcurrency() {
        return ordered.stream().mapToInt(DownloadProvider::getMaxConcurrency).min().orElse(8);
    }

    private interface Extractor {
        List<String> apply(DownloadProvider provider);
    }

    private List<String> merge(Extractor extractor) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (DownloadProvider p : ordered) {
            out.addAll(extractor.apply(p));
        }
        return new ArrayList<>(out);
    }
}
