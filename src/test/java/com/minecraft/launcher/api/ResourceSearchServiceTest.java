package com.minecraft.launcher.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceSearchServiceTest {

    private static SearchResult sample(String source, String name) {
        return new SearchResult(source + "-1", name, "", source, "mod", 10, List.of(), null);
    }

    private static final class FakeProvider implements ModProvider {
        private final String name;
        private final List<SearchResult> results;
        private final boolean boom;

        FakeProvider(String name, List<SearchResult> results, boolean boom) {
            this.name = name;
            this.results = results;
            this.boom = boom;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public List<SearchResult> search(String query, String projectType, int limit) {
            if (boom) {
                throw new RuntimeException("模拟来源故障");
            }
            return results;
        }
    }

    @Test
    void mergesResultsAndToleratesFailingProvider() {
        ResourceSearchService service = new ResourceSearchService(List.of(
                new FakeProvider("a", List.of(sample("a", "Alpha")), false),
                new FakeProvider("b", List.of(), true),
                new FakeProvider("c", List.of(sample("c", "Charlie")), false)
        ));

        List<SearchResult> merged = service.search("query", "mod", 10);
        assertEquals(2, merged.size());
        assertTrue(merged.stream().anyMatch(r -> r.name().equals("Alpha")));
        assertTrue(merged.stream().anyMatch(r -> r.name().equals("Charlie")));
    }

    @Test
    void emptyProvidersReturnsEmpty() {
        assertTrue(new ResourceSearchService(List.of()).search("q", "mod", 5).isEmpty());
    }
}
