package com.minecraft.launcher.backend;

import java.util.List;

/** 首页快照。 */
public final class HomeSnapshot {

    private final String welcomeTitle;
    private final String welcomeSubtitle;
    private final String profileName;
    private final boolean profileOnline;
    private final String profileTagline;
    private final String instanceName;
    private final String instanceDescription;
    private final String version;
    private final String loader;
    private final String javaVersion;
    private final List<String> modeTags;
    private final boolean ready;
    private final int resourceCount;
    private final double totalSizeGb;
    private final String lastPlayed;
    private final boolean systemHealthy;
    private final List<RecentInstance> recentInstances;

    public HomeSnapshot(String welcomeTitle, String welcomeSubtitle, String profileName, boolean profileOnline,
                        String profileTagline, String instanceName, String instanceDescription, String version,
                        String loader, String javaVersion, List<String> modeTags, boolean ready,
                        int resourceCount, double totalSizeGb, String lastPlayed, boolean systemHealthy,
                        List<RecentInstance> recentInstances) {
        this.welcomeTitle = welcomeTitle;
        this.welcomeSubtitle = welcomeSubtitle;
        this.profileName = profileName;
        this.profileOnline = profileOnline;
        this.profileTagline = profileTagline;
        this.instanceName = instanceName;
        this.instanceDescription = instanceDescription;
        this.version = version;
        this.loader = loader;
        this.javaVersion = javaVersion;
        this.modeTags = modeTags;
        this.ready = ready;
        this.resourceCount = resourceCount;
        this.totalSizeGb = totalSizeGb;
        this.lastPlayed = lastPlayed;
        this.systemHealthy = systemHealthy;
        this.recentInstances = recentInstances;
    }

    public String getWelcomeTitle() { return welcomeTitle; }
    public String getWelcomeSubtitle() { return welcomeSubtitle; }
    public String getProfileName() { return profileName; }
    public boolean getProfileOnline() { return profileOnline; }
    public String getProfileTagline() { return profileTagline; }
    public String getInstanceName() { return instanceName; }
    public String getInstanceDescription() { return instanceDescription; }
    public String getVersion() { return version; }
    public String getLoader() { return loader; }
    public String getJavaVersion() { return javaVersion; }
    public List<String> getModeTags() { return modeTags; }
    public boolean getReady() { return ready; }
    public int getResourceCount() { return resourceCount; }
    public double getTotalSizeGb() { return totalSizeGb; }
    public String getLastPlayed() { return lastPlayed; }
    public boolean getSystemHealthy() { return systemHealthy; }
    public List<RecentInstance> getRecentInstances() { return recentInstances; }
}
