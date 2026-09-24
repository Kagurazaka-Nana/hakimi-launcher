package com.minecraft.launcher.model.version.libraries;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class Download {

    private final Artifact artifact;

    /** 按 classifier 名索引的附加构件（原生库等），如 "natives-windows"。 */
    private final Map<String, Artifact> classifiers;

    public Download(@JsonProperty("artifact") Artifact artifact,
                    @JsonProperty("classifiers") Map<String, Artifact> classifiers) {
        this.artifact = artifact;
        this.classifiers = classifiers;
    }

}
