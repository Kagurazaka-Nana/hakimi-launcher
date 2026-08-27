package com.minecraft.launcher.model.version.downloads;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class Downloads {

    private final DownloadType client;
    private final DownloadType server;

    public Downloads(@JsonProperty("client") DownloadType client,
                     @JsonProperty("server") DownloadType server) {
        this.client = client;
        this.server = server;
    }

}
