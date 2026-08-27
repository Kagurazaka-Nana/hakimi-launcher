package com.minecraft.launcher.model.version;

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
    private final VersionType versionType;

}
