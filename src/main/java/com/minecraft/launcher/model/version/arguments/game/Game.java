package com.minecraft.launcher.model.version.arguments.game;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.minecraft.launcher.model.rule.Rule;
import lombok.Getter;

import java.util.List;

@Getter
public class Game {

    private final List<Game> games;
    private final List<Rule> rules;

    public Game(@JsonProperty("xxx") List<Game> games,
                @JsonProperty("rules") List<Rule> rules) {
        this.games = games;
        this.rules = rules;
    }

}
