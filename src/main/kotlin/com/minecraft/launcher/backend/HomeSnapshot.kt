package com.minecraft.launcher.backend

/** 首页快照：UI 只消费这个数据结构，真实来源后续由后端填充。 */
data class HomeSnapshot(
    val welcomeTitle: String,
    val welcomeSubtitle: String,
    val profileName: String,
    val profileBadge: String,
    val profileOnline: Boolean,
    val profileTagline: String,
    val instanceName: String,
    val instanceDescription: String,
    val version: String,
    val loader: String,
    val modeTags: List<String>,
    val quickActions: List<QuickAction>,
    val loadingPercent: Int,
    val loadingText: String,
    val loadingHint: String,
    val recentPlay: RecentPlay,
    val resourceStatus: List<ResourceRow>,
)

/** 快速操作磁贴。 */
data class QuickAction(
    val icon: String,
    val label: String,
    val colorHex: Long,
)

/** 最近游玩条目。 */
data class RecentPlay(
    val name: String,
    val detail: String,
    val time: String,
)

/** 资源状态行。 */
data class ResourceRow(
    val icon: String,
    val label: String,
    val ready: Boolean,
    val colorHex: Long,
)
