package com.minecraft.launcher.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Modrinth 资源提供方。读接口无需鉴权，参考其 v2 search API：
 * GET https://api.modrinth.com/v2/search?query=&facets=&limit=&index=relevance
 */
public final class ModrinthProvider implements ModProvider {

    private static final String BASE = "https://api.modrinth.com/v2/search";
    private static final String SOURCE = "modrinth";

    private final HttpTextFetcher http;
    private final ObjectMapper mapper = new ObjectMapper();

    public ModrinthProvider() {
        this(new HttpJsonClient());
    }

    public ModrinthProvider(HttpTextFetcher http) {
        this.http = http;
    }

    @Override
    public String name() {
        return SOURCE;
    }

    @Override
    public List<SearchResult> search(String query, String projectType, int limit) {
        Map<String, String> facets = new LinkedHashMap<>();
        facets.put("query", query == null ? "" : query);
        facets.put("limit", String.valueOf(Math.max(1, Math.min(limit, 100))));
        facets.put("index", "relevance");
        String type = normalizeType(projectType);
        if (type != null) {
            facets.put("facets", "[[\"project_type:" + type + "\"]]");
        }
        String url = HttpJsonClient.withQuery(BASE, facets);
        String body = http.get(url, Map.of());
        return parse(body);
    }

    /** 解析 Modrinth 搜索响应 JSON；供离线测试直接调用。 */
    public List<SearchResult> parse(String json) {
        List<SearchResult> results = new ArrayList<>();
        try {
            JsonNode root = mapper.readTree(json);
            JsonNode hits = root.path("hits");
            if (!hits.isArray()) {
                return results;
            }
            for (JsonNode hit : hits) {
                List<String> categories = new ArrayList<>();
                JsonNode cats = hit.path("categories");
                if (cats.isArray()) {
                    cats.forEach(c -> categories.add(c.asText()));
                }
                results.add(new SearchResult(
                        text(hit, "project_id", text(hit, "slug", "")),
                        text(hit, "title", ""),
                        text(hit, "description", ""),
                        SOURCE,
                        text(hit, "project_type", ""),
                        hit.path("downloads").asLong(-1),
                        categories,
                        text(hit, "icon_url", null)
                ));
            }
        } catch (Exception e) {
            // 解析失败返回已收集结果，保证调用方稳定
        }
        return results;
    }

    private static String normalizeType(String projectType) {
        if (projectType == null) {
            return null;
        }
        return switch (projectType.toLowerCase()) {
            case "mod", "mods" -> "mod";
            case "modpack", "modpacks", "整合包" -> "modpack";
            case "resourcepack", "resource", "资源包" -> "resourcepack";
            case "shader", "shaders", "光影" -> "shader";
            case "datapack", "data", "数据包" -> "datapack";
            default -> null;
        };
    }

    private static String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? fallback : value.asText();
    }
}
