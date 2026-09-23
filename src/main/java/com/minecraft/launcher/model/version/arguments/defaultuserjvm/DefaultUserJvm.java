package com.minecraft.launcher.model.version.arguments.defaultuserjvm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.minecraft.launcher.model.rule.Rule;
import lombok.Getter;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class DefaultUserJvm {

    private final List<Rule> rules;
    private final List<String> value;

    public DefaultUserJvm(@JsonProperty("rules") List<Rule> rules,
                          @JsonProperty("value") List<String> value) {
        this.rules = rules;
        this.value = value;
    }

}
