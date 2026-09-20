package com.minecraft.launcher.download;

/**
 * 分块下载的可注入抽象：真实实现走 HTTP Range 请求，
 * 单元测试可注入内存实现以离线验证并行/续传/校验逻辑。
 */
public interface RangeFetcher {

    /** 资源总字节数；无法获知时返回 -1。 */
    long size(String url);

    /** 获取 [start, end] 闭区间字节。 */
    byte[] fetch(String url, long start, long end);
}
