package com.minecraft.launcher.model.version;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.minecraft.launcher.model.manifest.VersionType;
import com.minecraft.launcher.model.version.arguments.Arguments;
import com.minecraft.launcher.model.version.assetindex.AssetIndex;
import com.minecraft.launcher.model.version.downloads.Downloads;
import com.minecraft.launcher.model.version.javaversion.JavaVersion;
import com.minecraft.launcher.model.version.libraries.Library;
import com.minecraft.launcher.model.version.logging.Logging;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Builder
@Jacksonized
public class VersionMeta {

    private final Arguments arguments;
    private final AssetIndex assetIndex;
    private final String assets;
    private final Integer complianceLevel;
    private final Downloads downloads;

    // clientVersion means id
    @JsonProperty("id")
    private final String clientVersion;
    private final JavaVersion javaVersion;
    private final List<Library> libraries;
    private final Logging logging;
    private final String mainClass;
    private final Integer minimumLauncherVersion;
    private final String releaseTime;
    private final String time;

    // 官方 JSON 字段名是 type（见 VersionInfo 的映射），不加 @JsonProperty 永远解析为 null
    @JsonProperty("type")
    private final VersionType versionType;

    /** 加载器版本 JSON 指向的原版 id；null 表示自身即原版。 */
    @JsonProperty("inheritsFrom")
    private final String inheritsFrom;

    /** 复用另一 id 的客户端 JAR（如 fabric 复用原版 jar）；null 表示使用自身 id 的 JAR。 */
    @JsonProperty("jar")
    private final String jar;

}
