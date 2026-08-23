package com.minecraft.launcher.versionmanifest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum VersionType {

    @JsonProperty("release") RELEASE,
    @JsonProperty("snapshot") SNAPSHOT,
    @JsonProperty("old_beta") OLD_BETA,
    @JsonProperty("old_alpha") OLD_ALPHA

}
