package com.minecraft.launcher.model.manifest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Optional;

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

    public Optional<VersionInfo> findVersionById(String id) {
        if (id == null || versions == null) {
            return Optional.empty();
        }
        return versions.stream()
                .filter(v -> id.equalsIgnoreCase(v.getId()))
                .findFirst();
    }

    public Optional<VersionInfo> getLatestRelease() {
        if (latest == null || latest.getRelease() == null) {
            return Optional.empty();
        }
        return findVersionById(latest.getRelease());
    }

    public VersionManifest(@JsonProperty("latest") Latest latest,
                           @JsonProperty("versions") List<VersionInfo> versions) {
        this.latest = latest;
        this.versions = versions;
    }

}
