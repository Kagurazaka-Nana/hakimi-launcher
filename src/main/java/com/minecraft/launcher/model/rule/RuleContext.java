package com.minecraft.launcher.model.rule;

import com.minecraft.launcher.util.OSVersionUtil;
import com.minecraft.launcher.util.PlatformUtil;

import java.util.Map;

public class RuleContext {

    private static final  RuleContext SYSTEM_CONTEXT = buildSystemContext();

    private final String osName;
    private final String osVersion;
    private final String arch;
    private final Map<String, Boolean> features;

    public RuleContext(String osName,
                       String osVersion,
                       String arch,
                       Map<String, Boolean> features) {
        this.osName = osName;
        this.osVersion = osVersion;
        this.arch = arch;
        this.features = (features == null || features.isEmpty())
                    ? Map.of()
                    : Map.copyOf(features);
    }

    public static RuleContext fromSystem() {
        return SYSTEM_CONTEXT;
    }

    private static RuleContext buildSystemContext() {

        String normalizedOsName = PlatformUtil.normalizeOsName(System.getProperty("os.name", ""));
        String normalizedOsVersion = OSVersionUtil.getExactVersion();
        String normalizedArch = PlatformUtil.normalizeArch(System.getProperty("os.arch", ""));

        return new RuleContext(normalizedOsName, normalizedOsVersion, normalizedArch, Map.of());
    }

    public RuleContext withFeatures(Map<String, Boolean> featureMap) {
        return new RuleContext(osName, osVersion, arch, featureMap);
    }

    public String getOsName() {
        return osName;
    }
    public String getOsVersion() {
        return osVersion;
    }
    public String getArch() {
        return arch;
    }
    public Map<String, Boolean> getFeatures() {
        return features;
    }

}
