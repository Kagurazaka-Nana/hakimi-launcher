package com.minecraft.launcher.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProvidersParseTest {

    @Test
    void modrinthParsesHits() {
        String json = """
            {
              "hits": [
                {"project_id":"mod1","slug":"sodium","title":"Sodium","description":"渲染引擎",
                 "downloads":2400000,"categories":["performance","optimization"],"project_type":"mod","icon_url":"https://cdn/icon.png"},
                {"project_id":"mod2","title":"Iris","description":"光影","downloads":1200000,"categories":[],"project_type":"mod"}
              ],
              "total_hits":2
            }
            """;
        List<SearchResult> results = new ModrinthProvider().parse(json);
        assertEquals(2, results.size());
        assertEquals("mod1", results.get(0).id());
        assertEquals("Sodium", results.get(0).name());
        assertEquals(2400000L, results.get(0).downloads());
        assertTrue(results.get(0).categories().contains("performance"));
        assertEquals("modrinth", results.get(0).source());
    }

    @Test
    void modrinthHandlesEmptyAndMalformed() {
        assertTrue(new ModrinthProvider().parse("{}").isEmpty());
        assertTrue(new ModrinthProvider().parse("not json").isEmpty());
    }

    @Test
    void curseforgeParsesData() {
        String json = """
            {
              "data": [
                {"id":123,"name":"JEI","summary":"物品查询","downloadCount":5000000,
                 "categories":[{"name":"Utility"},{"name":"QoL"}],"logo":{"url":"https://cf/logo.png"}}
              ]
            }
            """;
        List<SearchResult> results = new CurseForgeProvider(null, "dummy-key").parse(json);
        assertEquals(1, results.size());
        assertEquals("123", results.get(0).id());
        assertEquals("JEI", results.get(0).name());
        assertEquals(5000000L, results.get(0).downloads());
        assertTrue(results.get(0).categories().contains("Utility"));
        assertEquals("curseforge", results.get(0).source());
    }

    @Test
    void curseforgeWithoutKeyReturnsEmpty() {
        // 无 API key 时 search 直接返回空，不发起网络请求
        assertTrue(new CurseForgeProvider(null, null).search("sodium", "mod", 10).isEmpty());
        assertTrue(new CurseForgeProvider(null, "  ").search("sodium", "mod", 10).isEmpty());
    }

    @Test
    void mcmodParsesClassLinks() {
        String html = """
            <div class="result">
              <a href="https://www.mcmod.cn/class/3294.html">Sodium (钠) - 性能优化模组</a>
              <a href="https://www.mcmod.cn/class/3294.html">重复链接</a>
              <a href="https://www.mcmod.cn/class/100.html">Iris</a>
              <a href="https://www.mcmod.cn/item/1.html">非class链接应忽略</a>
            </div>
            """;
        List<SearchResult> results = new McModProvider().parseHtml(html, 10);
        assertEquals(2, results.size());
        assertEquals("mcmod-3294", results.get(0).id());
        assertTrue(results.get(0).name().contains("Sodium"));
        assertEquals("mcmod", results.get(0).source());
    }
}
