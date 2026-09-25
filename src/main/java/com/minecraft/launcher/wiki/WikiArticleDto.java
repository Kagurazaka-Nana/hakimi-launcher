package com.minecraft.launcher.wiki;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Wiki 列表条目（WebSocket {@code articles} 帧 items 元素，docs/Wiki.md §3.2）。 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WikiArticleDto(String id, String title, String category, String summary, String updatedAt) {
}
