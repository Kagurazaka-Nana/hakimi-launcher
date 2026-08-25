package com.minecraft.launcher.model.rule;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Features {

    private final Boolean isDemoUser;
    private final Boolean hasCustomResolution;
    private final Boolean hasQuickPlaysSupport;
    private final Boolean isQuickPlaySingleplayer;
    private final Boolean isQuickPlayMultiplayer;
    private final Boolean isQuickPlayRealms;

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
