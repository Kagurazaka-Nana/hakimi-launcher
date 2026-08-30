package com.minecraft.launcher.model.version.arguments.game;

import com.minecraft.launcher.model.rule.Rule;
import com.minecraft.launcher.model.version.arguments.AbstractListOrStringArgumentDeserializer;

import java.util.List;

public class GameDeserializer extends AbstractListOrStringArgumentDeserializer<Game> {

    @Override
    protected Game build(List<String> value, List<Rule> rules) {
        return new Game(value, rules);
    }

}
