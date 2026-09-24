package com.minecraft.launcher.download;

/** 下载进度快照。 */
public record DownloadProgress(
        DownloadState state,
        long downloadedBytes,
        /** 文件总大小；-1 表示服务器未给出（流式下载）。 */
        long totalBytes,
        /** 滑动窗口测得的实时速率（字节/秒）。 */
        long bytesPerSec,
        /** 当前活跃连接数。 */
        int activeConnections) {

    public static DownloadProgress connecting() {
        return new DownloadProgress(DownloadState.CONNECTING, 0, -1, 0, 0);
    }
}
