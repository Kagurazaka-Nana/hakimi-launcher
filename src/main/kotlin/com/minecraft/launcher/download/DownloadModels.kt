package com.minecraft.launcher.download

import java.time.Duration

/** 下载器配置。 */
data class DownloadConfig(
    /** 单任务并发连接数：8~16 是甜点区，再多通常触发服务端限流。 */
    val connections: Int = 8,
    /** 分片最小体积（字节）：动态切分不会低于该值。 */
    val minSegmentSize: Long = 1L shl 20,
    /** 初始分片数 = connections * initialSegmentsPerConnection。 */
    val initialSegmentsPerConnection: Int = 4,
    /** 单次网络读的缓冲大小（字节）。 */
    val bufferSize: Int = 256 shl 10,
    /** 限速（字节/秒），0 = 不限速。 */
    val maxBytesPerSec: Long = 0,
    val connectTimeout: Duration = Duration.ofSeconds(15),
    /** 片表元数据定时落盘间隔（不对每个网络块都写盘）。 */
    val metaFlushInterval: Duration = Duration.ofSeconds(2),
    /** 进度流发射间隔。 */
    val progressInterval: Duration = Duration.ofMillis(300),
) {
    init {
        require(connections in 1..64) { "connections 应在 1..64" }
        require(minSegmentSize > 0) { "minSegmentSize 必须为正" }
        require(bufferSize >= 8192) { "bufferSize 至少 8KB" }
        require(maxBytesPerSec >= 0) { "maxBytesPerSec 不能为负" }
    }
}

/** 下载状态。 */
enum class DownloadState {
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

/** 下载进度快照。 */
data class DownloadProgress(
    val state: DownloadState,
    val downloadedBytes: Long,
    /** 文件总大小；-1 表示服务器未给出（流式下载）。 */
    val totalBytes: Long,
    /** 滑动窗口测得的实时速率（字节/秒）。 */
    val bytesPerSec: Long,
    /** 当前活跃连接数。 */
    val activeConnections: Int,
)

/**
 * 一个待传输区间 [start, endInclusive]；endInclusive = -1 表示未知长度（读到 EOF）。
 * 动态分片调度的工作单元：worker 从 Channel 领取，谁快谁多取；跑得慢的片把剩余区间对半切推回队列。
 */
data class Segment(val start: Long, val endInclusive: Long) {
    val size: Long
        get() = if (endInclusive < 0) -1 else endInclusive - start + 1
}
