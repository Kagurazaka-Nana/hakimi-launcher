package com.minecraft.launcher.model.version.downloads;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class DownloadType {

    private final String sha1;
    private final Integer size;
    private final String url;

    public DownloadType(@JsonProperty("sha1") String sha1,
                        @JsonProperty("size") Integer size,
                        @JsonProperty("url") String url) {
        this.sha1 = sha1;
        this.size = size;
        this.url = url;
    }

}
