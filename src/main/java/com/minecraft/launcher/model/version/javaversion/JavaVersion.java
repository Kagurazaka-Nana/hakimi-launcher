package com.minecraft.launcher.model.version.javaversion;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class JavaVersion {

    private final String component;
    private final Integer majorVersion;

    public JavaVersion(@JsonProperty("component") String component,
                       @JsonProperty("majorVersion") Integer majorVersion) {
        this.component = component;
        this.majorVersion = majorVersion;
    }

}
