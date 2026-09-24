package com.minecraft.launcher.download.mojang;

import java.nio.file.Path;

/** .minecraft 目录布局（docs/DownloadPipeline.md §2）。 */
public final class GameLayout {

    private final Path root;

    public GameLayout(Path root) {
        this.root = root;
    }

    public Path getRoot() {
        return root;
    }

    public Path versionJar(String id) {
        return root.resolve("versions").resolve(id).resolve(id + ".jar");
    }

    public Path versionJson(String id) {
        return root.resolve("versions").resolve(id).resolve(id + ".json");
    }

    public Path library(String relativePath) {
        return root.resolve("libraries").resolve(relativePath);
    }

    public Path assetIndex(String id) {
        return root.resolve("assets").resolve("indexes").resolve(id + ".json");
    }

    /** @param location 形如 &lt;hash 前2位&gt;/&lt;hash&gt; */
    public Path assetObject(String location) {
        return root.resolve("assets").resolve("objects").resolve(location);
    }

    public Path runtime(String component, String platform) {
        return root.resolve("runtime").resolve(component).resolve(platform);
    }
}
