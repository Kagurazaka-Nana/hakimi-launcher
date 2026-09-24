package com.minecraft.launcher.backend;

import com.minecraft.launcher.download.DownloadState;

import java.util.Objects;

/** 下载队列中的单个任务（底部下载指示器展示用；含历史）。 */
public final class DownloadTask {

    private final String id;
    private final String name;
    private final String url;
    /** 0f..1f */
    private final float fraction;
    private final DownloadState state;

    public DownloadTask(String id, String name, String url, float fraction, DownloadState state) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.fraction = fraction;
        this.state = state;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getUrl() { return url; }
    public float getFraction() { return fraction; }
    public DownloadState getState() { return state; }

    public Builder toBuilder() {
        return new Builder().id(id).name(name).url(url).fraction(fraction).state(state);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String id;
        private String name;
        private String url;
        private float fraction;
        private DownloadState state;

        public Builder id(String v) { id = v; return this; }
        public Builder name(String v) { name = v; return this; }
        public Builder url(String v) { url = v; return this; }
        public Builder fraction(float v) { fraction = v; return this; }
        public Builder state(DownloadState v) { state = v; return this; }

        public DownloadTask build() {
            return new DownloadTask(id, name, url, fraction, state);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DownloadTask t)) return false;
        return Float.compare(t.fraction, fraction) == 0
                && Objects.equals(id, t.id) && Objects.equals(name, t.name)
                && Objects.equals(url, t.url) && state == t.state;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, url, fraction, state);
    }

    @Override
    public String toString() {
        return "DownloadTask(" + id + ", " + name + ", " + state + ", " + fraction + ")";
    }
}
