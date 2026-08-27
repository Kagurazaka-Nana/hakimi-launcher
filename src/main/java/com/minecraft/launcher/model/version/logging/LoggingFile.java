package com.minecraft.launcher.model.version.logging;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class LoggingFile {

    private final String id;
    private final String sha1;
    private final Integer size;
    private final String url;

    public LoggingFile(@JsonProperty("id") String id,
                       @JsonProperty("sha1") String sha1,
                       @JsonProperty("size") Integer size,
                       @JsonProperty("url") String url) {
        this.id = id;
        this.sha1 = sha1;
        this.size = size;
        this.url = url;
    }

}
