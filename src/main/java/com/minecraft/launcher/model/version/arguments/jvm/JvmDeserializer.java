package com.minecraft.launcher.model.version.arguments.jvm;

import com.minecraft.launcher.model.rule.Rule;
import com.minecraft.launcher.model.version.arguments.AbstractListOrStringArgumentDeserializer;

import java.util.List;

public class JvmDeserializer extends AbstractListOrStringArgumentDeserializer<Jvm> {

    @Override
    protected Jvm build(List<String> value, List<Rule> rules) {
        return new Jvm(value, rules);
    }

}
