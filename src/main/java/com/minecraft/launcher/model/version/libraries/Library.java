package com.minecraft.launcher.model.version.libraries;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.minecraft.launcher.model.rule.Rule;
import lombok.Getter;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class Library {

    private final Download downloads;
    private final String name;
    private final List<Rule> rules;

    public Library(@JsonProperty("downloads") Download downloads,
                   @JsonProperty("name") String name,
                   @JsonProperty("rules") List<Rule> rules) {
        this.downloads = downloads;
        this.name = name;
        this.rules = rules;
    }

}
