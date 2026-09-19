package com.minecraft.launcher.backend

/** 首页快照：UI 只消费这个数据结构，真实来源后续由后端填充。 */
data class HomeSnapshot(
    val username: String,
    val profileType: String,
    val worldName: String,
    val worldDescription: String,
    val worldMode: String,
    val ready: Boolean,
    val instanceLabel: String,
    val launchVersion: String,
    val modTags: List<String>,
    val modCount: Int,
    val instanceSize: String,
    val memoryUsedGb: Double,
    val memoryTotalGb: Double,
    val javaVersion: String,
    val javaOk: Boolean,
    val loaderName: String,
    val loaderVersion: String,
    val loaderCompatible: Boolean,
    val recentActivities: List<ActivityItem>,
)

/** 最近活动条目。 */
data class ActivityItem(
    val icon: String,
    val text: String,
    val time: String,
    val colorHex: Long,
)
