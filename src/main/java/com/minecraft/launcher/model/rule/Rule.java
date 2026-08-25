package com.minecraft.launcher.model.rule;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Rule {

    private final Action action;
    private final Features features;
    private final OperatingSystem os;

    public Rule(@JsonProperty("action") Action action,
                @JsonProperty("features") Features features,
                @JsonProperty("os") OperatingSystem os) {
        this.action = action;
        this.features = features;
        this.os = os;
    }

    public Action getAction() {
        return action;
    }
    public Features getFeatures() {
        return features;
    }
    public OperatingSystem getOs() {
        return os;
    }

}
