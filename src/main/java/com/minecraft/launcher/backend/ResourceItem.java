package com.minecraft.launcher.backend;

import java.util.List;
import java.util.Objects;

/** 资源条目（Mods / 资源包 / 数据包 / 光影 / 整合包 / 插件 通用）。 */
public final class ResourceItem {

    private final String id;
    private final String name;
    private final String summary;
    private final String description;
    private final String version;
    private final long downloads;
    private final List<String> categories;
    private final String author;
    /** 网络图片地址；null 表示无图标。 */
    private final String iconUrl;
    private final boolean enabled;

    public ResourceItem(String id, String name, String summary, String description, String version,
                        long downloads, List<String> categories, String author, String iconUrl, boolean enabled) {
        this.id = id;
        this.name = name;
        this.summary = summary;
        this.description = description;
        this.version = version;
        this.downloads = downloads;
        this.categories = categories;
        this.author = author;
        this.iconUrl = iconUrl;
        this.enabled = enabled;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getSummary() { return summary; }
    public String getDescription() { return description; }
    public String getVersion() { return version; }
    public long getDownloads() { return downloads; }
    public List<String> getCategories() { return categories; }
    public String getAuthor() { return author; }
    public String getIconUrl() { return iconUrl; }
    public boolean getEnabled() { return enabled; }

    public Builder toBuilder() {
        return new Builder().id(id).name(name).summary(summary).description(description)
                .version(version).downloads(downloads).categories(categories).author(author)
                .iconUrl(iconUrl).enabled(enabled);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String id;
        private String name;
        private String summary;
        private String description;
        private String version;
        private long downloads;
        private List<String> categories;
        private String author;
        private String iconUrl;
        private boolean enabled;

        public Builder id(String v) { id = v; return this; }
        public Builder name(String v) { name = v; return this; }
        public Builder summary(String v) { summary = v; return this; }
        public Builder description(String v) { description = v; return this; }
        public Builder version(String v) { version = v; return this; }
        public Builder downloads(long v) { downloads = v; return this; }
        public Builder categories(List<String> v) { categories = v; return this; }
        public Builder author(String v) { author = v; return this; }
        public Builder iconUrl(String v) { iconUrl = v; return this; }
        public Builder enabled(boolean v) { enabled = v; return this; }

        public ResourceItem build() {
            return new ResourceItem(id, name, summary, description, version, downloads, categories, author, iconUrl, enabled);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourceItem r)) return false;
        return enabled == r.enabled && downloads == r.downloads
                && Objects.equals(id, r.id) && Objects.equals(name, r.name)
                && Objects.equals(summary, r.summary) && Objects.equals(description, r.description)
                && Objects.equals(version, r.version) && Objects.equals(categories, r.categories)
                && Objects.equals(author, r.author) && Objects.equals(iconUrl, r.iconUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, summary, description, version, downloads, categories, author, iconUrl, enabled);
    }

    @Override
    public String toString() {
        return "ResourceItem(" + id + ", " + name + ", " + version + ")";
    }
}
