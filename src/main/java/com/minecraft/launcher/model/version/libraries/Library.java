package com.minecraft.launcher.model.version.libraries;

import com.minecraft.launcher.model.rule.Rule;
import lombok.Getter;

import java.util.List;

@Getter
public class Library {

    private final Download downloads;
    private final String name;
    private final List<Rule> rules;

    public Library(Download downloads,
                   String name,
                   List<Rule> rules) {
        this.downloads = downloads;
        this.name = name;
        this.rules = rules;
    }

}
