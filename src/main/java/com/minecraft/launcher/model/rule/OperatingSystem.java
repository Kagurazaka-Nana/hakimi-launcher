package com.minecraft.launcher.model.rule;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OperatingSystem {

    private final String name;
    private final String version;
    private final VersionRange versionRange;
    private final String arch;

    public OperatingSystem(@JsonProperty("name") String name,
                           @JsonProperty("version") String version,
                           @JsonProperty("versionRange") VersionRange versionRange,
                           @JsonProperty("arch") String arch) {
        this.name = name;
        this.version = version;
        this.versionRange = versionRange;
        this.arch = arch;
    }

    public String getName() {
        return name;
    }
    public String getVersion() {
        return version;
    }
    public VersionRange getVersionRange() {
        return versionRange;
    }
    public String getArch() {
        return arch;
    }

}
