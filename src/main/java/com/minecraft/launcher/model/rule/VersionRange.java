package com.minecraft.launcher.model.rule;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VersionRange {

    private final String max;
    private final String min;

    public VersionRange(@JsonProperty("max") String max,
                        @JsonProperty("min") String min) {
        this.max = max;
        this.min = min;
    }

    public String getMax() {
        return max;
    }
    public String getMin() {
        return min;
    }

}
