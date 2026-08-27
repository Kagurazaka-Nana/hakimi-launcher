package com.minecraft.launcher.model.version.assetindex;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class AssetIndex {
    private final String id;
    private final String sha1;
    private final Integer size;
    private final Integer totalSize;
    private final String url;

    public AssetIndex(@JsonProperty("id") String id,
                      @JsonProperty("sha1") String sha1,
                      @JsonProperty("size") Integer size,
                      @JsonProperty("totalSize") Integer totalSize,
                      @JsonProperty("url") String url) {
        this.id = id;
        this.sha1 = sha1;
        this.size = size;
        this.totalSize = totalSize;
        this.url = url;
    }

}
