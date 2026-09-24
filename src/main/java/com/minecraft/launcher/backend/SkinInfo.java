package com.minecraft.launcher.backend;

import java.util.Objects;

/** 皮肤信息。 */
public final class SkinInfo {

    private final String id;
    private final String name;
    private final boolean selected;

    public SkinInfo(String id, String name, boolean selected) {
        this.id = id;
        this.name = name;
        this.selected = selected;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public boolean getSelected() { return selected; }

    public Builder toBuilder() {
        return new Builder().id(id).name(name).selected(selected);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String id;
        private String name;
        private boolean selected;

        public Builder id(String v) { id = v; return this; }
        public Builder name(String v) { name = v; return this; }
        public Builder selected(boolean v) { selected = v; return this; }

        public SkinInfo build() {
            return new SkinInfo(id, name, selected);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SkinInfo s)) return false;
        return selected == s.selected && Objects.equals(id, s.id) && Objects.equals(name, s.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, selected);
    }
}
