package com.minecraft.launcher.api;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 聚合多个 {@link ModProvider} 的并发搜索：各来源并行请求，
 * 任一来源失败不影响其余来源（参考 HMCL 多源聚合思路）。
 */
public final class ResourceSearchService {

    private final List<ModProvider> providers;

    public ResourceSearchService(List<ModProvider> providers) {
        this.providers = List.copyOf(providers);
    }

    public static ResourceSearchService defaultService() {
        return new ResourceSearchService(List.of(
                new ModrinthProvider(),
                new CurseForgeProvider(),
                new McModProvider()
        ));
    }

    public List<SearchResult> search(String query, String projectType, int perProviderLimit) {
        List<SearchResult> merged = new ArrayList<>();
        if (providers.isEmpty()) {
            return merged;
        }
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(providers.size(), 8));
        try {
            List<Future<List<SearchResult>>> futures = new ArrayList<>();
            for (ModProvider provider : providers) {
                Callable<List<SearchResult>> task = () -> {
                    try {
                        return provider.search(query, projectType, perProviderLimit);
                    } catch (Exception e) {
                        return List.<SearchResult>of();
                    }
                };
                futures.add(pool.submit(task));
            }
            for (Future<List<SearchResult>> future : futures) {
                try {
                    merged.addAll(future.get(30, TimeUnit.SECONDS));
                } catch (Exception e) {
                    // 单来源超时/失败：跳过
                }
            }
        } finally {
            pool.shutdownNow();
        }
        return merged;
    }
}
