package com.minecraft.launcher.model.version.libraries;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/** 原生库解压选项：exclude 列出的路径在解压到 natives 目录时跳过。 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class Extract {

    private final List<String> exclude;

    public Extract(@JsonProperty("exclude") List<String> exclude) {
        this.exclude = exclude;
    }

}
