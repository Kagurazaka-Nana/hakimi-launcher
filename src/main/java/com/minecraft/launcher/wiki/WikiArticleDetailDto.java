package com.minecraft.launcher.wiki;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Wiki 词条正文（WebSocket {@code article} 帧，html 为服务端 Jsoup 清洗后的安全子集；version 为编辑乐观锁基准）。 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WikiArticleDetailDto(
        String id,
        String title,
        String category,
        String html,
        String author,
        String updatedAt,
        int version
) {
}
