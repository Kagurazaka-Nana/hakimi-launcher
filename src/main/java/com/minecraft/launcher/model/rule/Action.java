package com.minecraft.launcher.model.rule;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Action {

    @JsonProperty("allow")
    ALLOW,

    @JsonProperty("disallow")
    DISALLOW

}
