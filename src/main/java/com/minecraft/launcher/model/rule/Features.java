package com.minecraft.launcher.model.rule;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Features {

    private final Boolean isDemoUser;
    private final Boolean hasCustomResolution;
    private final Boolean hasQuickPlaysSupport;
    private final Boolean isQuickPlaySingleplayer;
    private final Boolean isQuickPlayMultiplayer;
    private final Boolean isQuickPlayRealms;

    private final Map<String, Boolean> featureMap;

    public Features(@JsonProperty("is_demo_user") Boolean isDemoUser,
                    @JsonProperty("has_custom_resolution") Boolean hasCustomResolution,
                    @JsonProperty("has_quick_plays_support") Boolean hasQuickPlaysSupport,
                    @JsonProperty("is_quick_play_singleplayer") Boolean isQuickPlaySingleplayer,
                    @JsonProperty("is_quick_play_multiplayer") Boolean isQuickPlayMultiplayer,
                    @JsonProperty("is_quick_play_realms") Boolean isQuickPlayRealms) {
        this.isDemoUser = isDemoUser;
        this.hasCustomResolution = hasCustomResolution;
        this.hasQuickPlaysSupport = hasQuickPlaysSupport;
        this.isQuickPlaySingleplayer = isQuickPlaySingleplayer;
        this.isQuickPlayMultiplayer = isQuickPlayMultiplayer;
        this.isQuickPlayRealms = isQuickPlayRealms;

        Map<String, Boolean> map = new HashMap<>(6);
        putIfNotNull(map, "is_demo_user", isDemoUser);
        putIfNotNull(map, "has_custom_resolution", hasCustomResolution);
        putIfNotNull(map, "has_quick_plays_support", hasQuickPlaysSupport);
        putIfNotNull(map, "is_quick_play_singleplayer", isQuickPlaySingleplayer);
        putIfNotNull(map, "is_quick_play_multiplayer", isQuickPlayMultiplayer);
        putIfNotNull(map, "is_quick_play_realms", isQuickPlayRealms);

        this.featureMap = Collections.unmodifiableMap(map);
    }

    private static void putIfNotNull(
            Map<String, Boolean> map,
            String key,
            Boolean value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    public Map<String, Boolean> toMap() {
        return featureMap;
    }

    public Boolean getIsDemoUser() {
        return isDemoUser;
    }
    public Boolean getHasCustomResolution() {
        return hasCustomResolution;
    }
    public Boolean getHasQuickPlaysSupport() {
        return hasQuickPlaysSupport;
    }
    public Boolean getIsQuickPlaySingleplayer() {
        return isQuickPlaySingleplayer;
    }
    public Boolean getIsQuickPlayMultiplayer() {
        return isQuickPlayMultiplayer;
    }
    public Boolean getIsQuickPlayRealms() {
        return isQuickPlayRealms;
    }

}
