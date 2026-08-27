package com.minecraft.launcher.model.version.libraries;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class Artifact {

    private final String path;
    private final String sha1;
    private final Integer size;
    private final String url;

    public Artifact(@JsonProperty("path") String path,
                    @JsonProperty("sha1") String sha1,
                    @JsonProperty("size") Integer size,
                    @JsonProperty("url") String url) {
        this.path = path;
        this.sha1 = sha1;
        this.size = size;
        this.url = url;
    }

}
