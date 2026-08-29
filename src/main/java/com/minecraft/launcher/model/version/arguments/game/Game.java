package com.minecraft.launcher.model.version.arguments.game;

import com.minecraft.launcher.model.rule.Rule;
import lombok.Getter;

import java.util.List;

@Getter
public class Game {

    private final Object value;
    private final List<Rule> rules;

    public Game(Object value,
                List<Rule> rules) {
        this.value = value;
        this.rules = rules;
    }

}
