package com.minecraft.launcher.versionmanifest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VersionInfo {

    private final String id;
    private final VersionType type;
    private final String url;
    private final String time;
    private final String releaseTime;

    // Reserved for version_manifest_v2.json
    private final String sha1;
    private final Integer complianceLevel;

    public String getId() {
        return this.id;
    }
    public VersionType getType() {
        return this.type;
    }
    public String getUrl() {
        return this.url;
    }
    public String getTime() {
        return this.time;
    }
    public String getReleaseTime() {
        return this.releaseTime;
    }
    public String getSha1() {
        return this.sha1;
    }
    public Integer getComplianceLevel() {
        return this.complianceLevel;
    }

    // 整型: complianceLevel（仅v2）：如果为0，启动器会警告用户此版本因老旧而不足以支持最新的玩家安全特性。其他情况为1。
    // https://zh.minecraft.wiki/w/Version_manifest.json#JSON%E6%A0%BC%E5%BC%8F
    public boolean isLegacy() {
        return this.complianceLevel != null && this.complianceLevel == 0;
    }

    public VersionInfo(@JsonProperty("id") String id,
                       @JsonProperty("type") VersionType type,
                       @JsonProperty("url") String url,
                       @JsonProperty("time") String time,
                       @JsonProperty("releaseTime") String releaseTime,
                       @JsonProperty("sha1") String sha1,
                       @JsonProperty("complianceLevel") Integer complianceLevel
                       ) {
        this.id = id;
        this.type = type;
        this.url = url;
        this.time = time;
        this.releaseTime = releaseTime;
        this.sha1 = sha1;
        this.complianceLevel = complianceLevel;
    }
}
