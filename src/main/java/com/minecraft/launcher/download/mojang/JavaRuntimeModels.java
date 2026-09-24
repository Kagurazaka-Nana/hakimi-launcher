package com.minecraft.launcher.download.mojang;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.minecraft.launcher.model.version.libraries.Artifact;
import lombok.Getter;

import java.util.Map;

/**
 * Java runtime 目录（all.json）与单 runtime 清单（manifest.json）的模型。
 * 目录为「平台 → 组件 → 信息」两级映射；清单的 files 为「相对路径 → 文件条目」，
 * downloads 键为 raw/lzma（P4 只用 raw）。
 */
public final class JavaRuntimeModels {

    private JavaRuntimeModels() {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    public static class CatalogEntry {
        private final ManifestPointer manifest;
        private final VersionInfo version;

        public CatalogEntry(@JsonProperty("manifest") ManifestPointer manifest,
                            @JsonProperty("version") VersionInfo version) {
            this.manifest = manifest;
            this.version = version;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        @Getter
        public static class ManifestPointer {
            private final String sha1;
            private final Integer size;
            private final String url;

            public ManifestPointer(@JsonProperty("sha1") String sha1,
                                   @JsonProperty("size") Integer size,
                                   @JsonProperty("url") String url) {
                this.sha1 = sha1;
                this.size = size;
                this.url = url;
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        @Getter
        public static class VersionInfo {
            private final String name;
            private final String released;

            public VersionInfo(@JsonProperty("name") String name,
                               @JsonProperty("released") String released) {
                this.name = name;
                this.released = released;
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    public static class Manifest {
        private final Map<String, FileEntry> files;

        public Manifest(@JsonProperty("files") Map<String, FileEntry> files) {
            this.files = files;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        @Getter
        public static class FileEntry {
            private final String type;          // file | directory
            private final Boolean executable;
            private final Map<String, Artifact> downloads; // raw | lzma

            public FileEntry(@JsonProperty("type") String type,
                             @JsonProperty("executable") Boolean executable,
                             @JsonProperty("downloads") Map<String, Artifact> downloads) {
                this.type = type;
                this.executable = executable;
                this.downloads = downloads;
            }

            public boolean isFile() {
                return "file".equals(type);
            }

            public boolean isExecutable() {
                return Boolean.TRUE.equals(executable);
            }

            public Artifact raw() {
                return downloads == null ? null : downloads.get("raw");
            }
        }
    }
}
