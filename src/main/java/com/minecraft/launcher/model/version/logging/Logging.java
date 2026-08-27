package com.minecraft.launcher.model.version.logging;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class Logging {

    private final LoggingClient client;

    public Logging(@JsonProperty("client") LoggingClient client) {
        this.client = client;
    }

}
