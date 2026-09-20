package com.minecraft.launcher.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProvidersParseTest {

    /** 记录请求 URL 并返回固定响应体的假 HTTP 层。 */
    private static final class FakeFetcher implements HttpTextFetcher {
        final String response;
        final boolean boom;
        String lastUrl;

        FakeFetcher(String response) {
            this(response, false);
        }

        FakeFetcher(String response, boolean boom) {
            this.response = response;
            this.boom = boom;
        }

        @Override
        public String get(String url, Map<String, String> headers) {
            lastUrl = url;
            if (boom) {
                throw new RuntimeException("network down");
            }
            return response;
        }
    }

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
    void modrinthSearchBuildsFacetedUrlAndParses() {
        FakeFetcher fetcher = new FakeFetcher("{\"hits\":[{\"project_id\":\"p1\",\"title\":\"T\",\"downloads\":5,\"categories\":[],\"project_type\":\"mod\"}]}");
        List<SearchResult> results = new ModrinthProvider(fetcher).search("sodium", "mod", 10);
        assertEquals(1, results.size());
        assertTrue(fetcher.lastUrl.startsWith("https://api.modrinth.com/v2/search"));
        assertTrue(fetcher.lastUrl.contains("project_type%3Amod"));
        assertTrue(fetcher.lastUrl.contains("query=sodium"));
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
    void curseforgeSearchUsesKeyAndClassId() {
        FakeFetcher fetcher = new FakeFetcher("{\"data\":[{\"id\":9,\"name\":\"X\",\"summary\":\"\",\"downloadCount\":1,\"categories\":[]}]}");
        List<SearchResult> results = new CurseForgeProvider(fetcher, "key123").search("jei", "modpack", 10);
        assertEquals(1, results.size());
        assertTrue(fetcher.lastUrl.contains("classId=4471"));
        assertTrue(fetcher.lastUrl.contains("gameId=432"));
    }

    @Test
    void curseforgeWithoutKeyReturnsEmpty() {
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

    @Test
    void mcmodSearchParsesAndToleratesFailure() {
        FakeFetcher ok = new FakeFetcher("<a href=\"https://www.mcmod.cn/class/7.html\">Foo</a>");
        assertEquals(1, new McModProvider(ok).search("foo", "mod", 10).size());

        FakeFetcher failing = new FakeFetcher("", true);
        assertTrue(new McModProvider(failing).search("foo", "mod", 10).isEmpty());
    }
}
