package com.minecraft.launcher.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MC百科（mcmod.cn）提供方。该站无公开 JSON API，参考 HMCL/PCL 采用搜索结果页 HTML 解析，
 * 属尽力而为：任何网络或解析异常都安全返回空列表，不影响其它来源。
 */
public final class McModProvider implements ModProvider {

    private static final String BASE = "https://www.mcmod.cn/s";
    private static final String SOURCE = "mcmod";
    private static final Pattern LINK = Pattern.compile(
            "<a[^>]+href=\"https?://www\\.mcmod\\.cn/class/(\\d+)\\.html\"[^>]*>([^<]+)</a>",
            Pattern.CASE_INSENSITIVE);

    private final HttpJsonClient http;

    public McModProvider() {
        this(new HttpJsonClient());
    }

    public McModProvider(HttpJsonClient http) {
        this.http = http;
    }

    @Override
    public String name() {
        return SOURCE;
    }

    @Override
    public List<SearchResult> search(String query, String projectType, int limit) {
        try {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("key", query == null ? "" : query);
            String url = HttpJsonClient.withQuery(BASE, params);
            String html = http.get(url, Map.of());
            return parseHtml(html, limit);
        } catch (Exception e) {
            // 该站可能限流或改版；失败不影响其它来源
            return List.of();
        }
    }

    /** 从搜索结果 HTML 中解析 class 链接与标题；供离线测试直接调用。 */
    public List<SearchResult> parseHtml(String html, int limit) {
        List<SearchResult> results = new ArrayList<>();
        if (html == null) {
            return results;
        }
        Map<String, Boolean> seen = new LinkedHashMap<>();
        Matcher matcher = LINK.matcher(html);
        while (matcher.find() && results.size() < Math.max(1, limit)) {
            String id = matcher.group(1);
            String title = matcher.group(2).trim();
            if (seen.putIfAbsent(id, Boolean.TRUE) != null) {
                continue;
            }
            results.add(new SearchResult(
                    "mcmod-" + id,
                    title,
                    "",
                    SOURCE,
                    "mod",
                    -1,
                    List.of(),
                    "https://www.mcmod.cn/class/" + id + ".html"
            ));
        }
        return results;
    }
}
