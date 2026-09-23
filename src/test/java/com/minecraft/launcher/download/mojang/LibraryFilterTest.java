package com.minecraft.launcher.download.mojang;

import com.fasterxml.jackson.core.type.TypeReference;
import com.minecraft.launcher.model.rule.RuleContext;
import com.minecraft.launcher.model.rule.RuleEvaluator;
import com.minecraft.launcher.model.version.libraries.Library;
import com.minecraft.launcher.util.JsonUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibraryFilterTest {

    private static final String LIBRARIES_JSON = """
            [
              {"name":"com.mojang:logging:1.1.1",
               "downloads":{"artifact":{"path":"com/mojang/logging/1.1.1/logging-1.1.1.jar",
                 "sha1":"aaa","size":100,"url":"https://piston-data.mojang.com/v1/objects/aaa/logging-1.1.1.jar"}}},
              {"name":"windows:only:1",
               "rules":[{"action":"allow","os":{"name":"windows"}}],
               "downloads":{"artifact":{"path":"windows/only/1/only-1.jar","sha1":"bbb","size":10,"url":"https://x.test/only-1.jar"}}},
              {"name":"lwjgl:glfw:3.3.3",
               "rules":[{"action":"allow"},{"action":"disallow","os":{"name":"osx"}}],
               "downloads":{
                 "artifact":{"path":"org/lwjgl/lwjgl-glfw/3.3.3/lwjgl-glfw-3.3.3.jar","sha1":"ccc","size":200,"url":"https://x.test/glfw.jar"},
                 "classifiers":{
                   "natives-linux":{"path":"org/lwjgl/lwjgl-glfw/3.3.3/lwjgl-glfw-3.3.3-natives-linux.jar","sha1":"ddd","size":50,"url":"https://x.test/glfw-natives.jar"},
                   "natives-windows":{"path":"org/lwjgl/lwjgl-glfw/3.3.3/lwjgl-glfw-3.3.3-natives-windows.jar","sha1":"eee","size":60,"url":"https://x.test/glfw-win.jar"}}},
               "natives":{"linux":"natives-linux","windows":"natives-windows"},
               "extract":{"exclude":["META-INF/"]}},
              {"name":"legacy:forge:1.0","url":"https://maven.minecraftforge.net/"}
            ]
            """;

    private List<Library> libs() throws IOException {
        return JsonUtils.fromJson(LIBRARIES_JSON, new TypeReference<List<Library>>() {});
    }

    private LibraryFilter filterFor(String osName) {
        return new LibraryFilter(new RuleEvaluator(new RuleContext(osName, "10.0", "x64", Map.of())));
    }

    @Test
    void selectsArtifactAndLinuxNative() throws IOException {
        List<LibraryDownload> selected = filterFor("linux").select(libs());

        List<String> paths = selected.stream().map(LibraryDownload::relativePath).toList();
        assertTrue(paths.contains("com/mojang/logging/1.1.1/logging-1.1.1.jar"));
        assertTrue(paths.contains("org/lwjgl/lwjgl-glfw/3.3.3/lwjgl-glfw-3.3.3-natives-linux.jar"));
        assertFalse(paths.contains("windows/only/1/only-1.jar"), "windows-only 规则应被过滤");
        assertFalse(paths.contains("org/lwjgl/lwjgl-glfw/3.3.3/lwjgl-glfw-3.3.3-natives-windows.jar"));
    }

    @Test
    void nativeEntryCarriesExtractExcludesAndFlag() throws IOException {
        List<LibraryDownload> selected = filterFor("linux").select(libs());
        LibraryDownload nativeEntry = selected.stream()
                .filter(d -> d.relativePath().endsWith("natives-linux.jar"))
                .findFirst().orElseThrow();
        assertTrue(nativeEntry.nativeArtifact());
        assertEquals(List.of("META-INF/"), nativeEntry.extractExclude());
        assertEquals("ddd", nativeEntry.sha1());
        assertEquals(50, nativeEntry.size());
    }

    @Test
    void legacyLibraryUsesMavenPathAndOwnBaseUrl() throws IOException {
        List<LibraryDownload> selected = filterFor("linux").select(libs());
        LibraryDownload legacy = selected.stream()
                .filter(d -> d.name().equals("legacy:forge:1.0"))
                .findFirst().orElseThrow();
        assertEquals("legacy/forge/1.0/forge-1.0.jar", legacy.relativePath());
        assertEquals("https://maven.minecraftforge.net/legacy/forge/1.0/forge-1.0.jar", legacy.url());
        assertFalse(legacy.nativeArtifact());
    }

    @Test
    void windowsSelectsWindowsNative() throws IOException {
        List<LibraryDownload> selected = filterFor("windows").select(libs());
        assertTrue(selected.stream().anyMatch(d -> d.relativePath().endsWith("natives-windows.jar")));
        assertTrue(selected.stream().anyMatch(d -> d.relativePath().contains("only-1.jar")), "windows 应包含 windows-only 库");
    }

    @Test
    void mavenPathHandlesClassifier() {
        assertEquals("a/b/2.1/b-2.1.jar", LibraryFilter.mavenPath("a:b:2.1"));
        assertEquals("a/b/2.1/b-2.1-natives-linux.jar", LibraryFilter.mavenPath("a:b:2.1:natives-linux"));
    }
}
