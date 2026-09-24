package com.minecraft.launcher.download;

/** 下载状态。 */
public enum DownloadState {
    /** 建立连接 / 探测服务器能力中。 */
    CONNECTING,
    /** 传输中。 */
    DOWNLOADING,
    /** 已完成（.part 已原子改名为目标文件）。 */
    COMPLETED,
    /** 被取消（= 暂停；.part 与片表保留，可断点续传）。 */
    CANCELLED,
    /** 失败（.part 与片表保留）。 */
    FAILED,
}
