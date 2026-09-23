package com.minecraft.launcher.download.mojang;

import java.util.List;

/**
 * 下载源抽象（参考 HMCL DownloadProvider）：镜像/源切换不是"以后再说"，
 * 而是第一层的 URL 解析策略——官方 JSON 里的 URL 不可改，改写发生在下载前。
 *
 * 所有返回的候选 URL 均为按尝试顺序排列的不可变列表；每个 URL 在下载前仍会经过
 * 下载引擎的 SSRF 校验（仅 http/https、拒绝内网/保留地址）。
 */
public interface DownloadProvider {

    /** 版本清单（version_manifest_v2.json）的候选 URL，按尝试顺序。 */
    List<String> getVersionListUrls();

    /**
     * 资源对象的候选 URL。
     *
     * @param assetObjectLocation 形如 {@code <hash 前2位>/<hash>} 的对象相对路径
     */
    List<String> getAssetObjectCandidates(String assetObjectLocation);

    /**
     * 把 Mojang/Forge JSON 里的原始 URL 改写为本源的候选 URL 列表。
     * 官方源即恒等映射；镜像源做前缀替换，可附带回退候选。
     */
    List<String> injectURLCandidates(String baseURL);

    /** 本源支持的最大并发（跨文件），供编排层限流。 */
    int getMaxConcurrency();
}
