package com.minecraft.launcher.download;

/** 下载进度回调。 */
@FunctionalInterface
public interface ProgressListener {

    /**
     * @param downloaded 已完成字节
     * @param total      总字节（未知时为 -1）
     */
    void onProgress(long downloaded, long total);
}
