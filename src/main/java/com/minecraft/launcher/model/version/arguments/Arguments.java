package com.minecraft.launcher.model.version.arguments;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.minecraft.launcher.model.version.arguments.defaultuserjvm.DefaultUserJvm;
import com.minecraft.launcher.model.version.arguments.game.Game;
import com.minecraft.launcher.model.version.arguments.jvm.Jvm;
import lombok.Getter;

import java.util.List;

@Getter
public class Arguments {

    private final List<DefaultUserJvm> defaultUserJvm;
    private final List<Game> game;
    private final List<Jvm> jvm;

    public Arguments(@JsonProperty("default-user-jvm") List<DefaultUserJvm> defaultUserJvm,
                     @JsonProperty("game") List<Game> game,
                     @JsonProperty("jvm") List<Jvm> jvm) {
        this.defaultUserJvm = defaultUserJvm;
        this.game = game;
        this.jvm = jvm;
    }

}
