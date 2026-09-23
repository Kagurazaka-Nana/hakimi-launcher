package com.minecraft.launcher.model.version.logging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class Logging {

    private final LoggingClient client;

    public Logging(@JsonProperty("client") LoggingClient client) {
        this.client = client;
    }

}
