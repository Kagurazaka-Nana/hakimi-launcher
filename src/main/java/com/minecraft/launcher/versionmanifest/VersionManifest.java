package com.minecraft.launcher.versionmanifest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VersionManifest {

    private final Latest latest;
    private final List<VersionInfo> versions;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Latest {
        private final String release;
        private final String snapshot;

        public String getRelease() {
            return this.release;
        }
        public String getSnapshot() {
            return this.snapshot;
        }

        public Latest(@JsonProperty("release") String release,
                      @JsonProperty("snapshot") String snapshot) {
            this.release = release;
            this.snapshot = snapshot;
        }
    }

    public Latest getLatest() {
        return this.latest;
    }
    public List<VersionInfo> getVersions() {
        return this.versions;
    }

    public VersionManifest(@JsonProperty("latest") Latest latest,
                           @JsonProperty("versions") List<VersionInfo> versions) {
        this.latest = latest;
        this.versions = versions;
    }

}
