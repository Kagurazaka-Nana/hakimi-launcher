package com.minecraft.launcher.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CurseForge Core 提供方（Minecraft gameId=432）。需要 x-api-key，
 * 通过环境变量 {@code CURSEFORGE_API_KEY} 或构造参数注入；缺 key 时 search 返回空。
 */
public final class CurseForgeProvider implements ModProvider {

    private static final String BASE = "https://api.curseforge.com/v1/mods/search";
    private static final int GAME_ID = 432;
    private static final String SOURCE = "curseforge";

    private final HttpTextFetcher http;
    private final String apiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public CurseForgeProvider() {
        this(new HttpJsonClient(), System.getenv("CURSEFORGE_API_KEY"));
    }

    public CurseForgeProvider(HttpTextFetcher http, String apiKey) {
        this.http = http;
        this.apiKey = apiKey;
    }

    @Override
    public String name() {
        return SOURCE;
    }

    @Override
    public List<SearchResult> search(String query, String projectType, int limit) {
        if (apiKey == null || apiKey.isBlank()) {
            return List.of();
        }
        Map<String, String> params = new LinkedHashMap<>();
        params.put("gameId", String.valueOf(GAME_ID));
        params.put("classId", String.valueOf(classIdFor(projectType)));
        params.put("searchFilter", query == null ? "" : query);
        params.put("pageSize", String.valueOf(Math.max(1, Math.min(limit, 50))));
        params.put("index", "0");
        params.put("sortField", "-1"); // popularity
        String url = HttpJsonClient.withQuery(BASE, params);
        String body = http.get(url, Map.of("x-api-key", apiKey));
        return parse(body);
    }

    /** 解析 CurseForge 搜索响应 JSON；供离线测试直接调用。 */
    public List<SearchResult> parse(String json) {
        List<SearchResult> results = new ArrayList<>();
        try {
            JsonNode root = mapper.readTree(json);
            JsonNode data = root.path("data");
            if (!data.isArray()) {
                return results;
            }
            for (JsonNode mod : data) {
                List<String> categories = new ArrayList<>();
                for (JsonNode cat : mod.path("categories")) {
                    String name = cat.path("name").asText(null);
                    if (name != null) {
                        categories.add(name);
                    }
                }
                results.add(new SearchResult(
                        String.valueOf(mod.path("id").asLong()),
                        mod.path("name").asText(""),
                        mod.path("summary").asText(""),
                        SOURCE,
                        "mod",
                        mod.path("downloadCount").asLong(-1),
                        categories,
                        mod.path("logo").path("url").asText(null)
                ));
            }
        } catch (Exception e) {
            // 解析失败返回已收集结果
        }
        return results;
    }

    private static int classIdFor(String projectType) {
        if (projectType == null) {
            return 6;
        }
        return switch (projectType.toLowerCase()) {
            case "modpack", "modpacks", "整合包" -> 4471;
            case "resourcepack", "resource", "资源包" -> 12;
            case "world", "worlds", "地图" -> 17;
            case "plugin", "plugins", "bukkit", "插件" -> 5;
            case "datapack", "数据包" -> 6145;
            default -> 6; // mods
        };
    }
}
