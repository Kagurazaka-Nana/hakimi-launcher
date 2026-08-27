package com.minecraft.launcher.model.version.logging;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class LoggingClient {

    private final String argument;
    private final LoggingFile file;
    private final String type;

    public LoggingClient(@JsonProperty("argument") String argument,
                         @JsonProperty("file") LoggingFile file,
                         @JsonProperty("type") String type) {
        this.argument = argument;
        this.file = file;
        this.type = type;
    }

}
