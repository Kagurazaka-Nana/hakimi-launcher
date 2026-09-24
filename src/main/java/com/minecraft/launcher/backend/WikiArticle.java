package com.minecraft.launcher.backend;

/** Wiki 文章。 */
public final class WikiArticle {

    private final String id;
    private final String title;
    private final String category;
    private final String excerpt;
    private final String updated;

    public WikiArticle(String id, String title, String category, String excerpt, String updated) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.excerpt = excerpt;
        this.updated = updated;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getExcerpt() { return excerpt; }
    public String getUpdated() { return updated; }
}
