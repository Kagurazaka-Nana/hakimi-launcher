package com.minecraft.launcher.model.manifest;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum VersionType {

    @JsonProperty("release") RELEASE,
    @JsonProperty("snapshot") SNAPSHOT,
    @JsonProperty("old_beta") OLD_BETA,
    @JsonProperty("old_alpha") OLD_ALPHA

}
