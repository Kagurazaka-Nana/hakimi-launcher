package com.minecraft.launcher.backend

/** 实例卡片数据。 */
data class InstanceItem(
    val name: String,
    val version: String,
    val loader: String,
    val modeTag: String,
    val playerTag: String,
    val description: String,
    val running: Boolean,
    val lastPlayed: String,
    val owner: String,
)

/** 下载条目。 */
data class DownloadItem(
    val name: String,
    val badge: String,
    val version: String,
    val loaders: String,
    val description: String,
    val size: String,
)

/** 下载队列任务。 */
data class DownloadTask(
    val name: String,
    val percent: Int?,
    val status: String,
    val detail: String,
    val action: String,
)

/** 下载页快照：分类、资源列表、队列。 */
data class DownloadsSnapshot(
    val categories: List<String>,
    val selectedCategory: String,
    val items: List<DownloadItem>,
    val queue: List<DownloadTask>,
)

/** 设置页快照。 */
data class SettingsSnapshot(
    val theme: String,
    val language: String,
    val javaPath: String,
    val javaVersion: String,
    val maxMemoryMb: Int,
    val memoryMinMb: Int,
    val memoryMaxMb: Int,
    val downloadSource: String,
    val concurrency: Int,
    val concurrencyMin: Int,
    val concurrencyMax: Int,
    val jvmArgs: String,
    val debugMode: Boolean,
    val account: String,
    val privacy: String,
)
