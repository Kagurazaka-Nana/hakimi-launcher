package com.minecraft.launcher.model.version.arguments.jvm;

import com.minecraft.launcher.model.rule.Rule;
import lombok.Getter;

import java.util.List;

@Getter
public class Jvm {

    private final List<String> value;
    private final List<Rule> rules;

    public Jvm(List<String> value,
               List<Rule> rules) {
        this.value = value;
        this.rules = rules;
    }
}
