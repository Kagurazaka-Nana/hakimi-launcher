package com.minecraft.launcher.model.version.libraries;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class Download {

    private final Artifact artifact;

    public Download(@JsonProperty("artifact") Artifact artifact) {
        this.artifact = artifact;
    }

}
