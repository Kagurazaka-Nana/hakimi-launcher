package com.minecraft.launcher.util;

import java.util.Locale;

public class PlatformUtil {

    public static String normalizeOsName(String rawOsName) {
        String name = rawOsName.toLowerCase(Locale.ROOT);

        if (name.contains("win")) {
            return "windows";
        }

        if (name.contains("mac") || name.contains("darwin")) {
            return "osx";
        }

        if (name.contains("nix") || name.contains("nux") || name.contains("aix")) {
            return "linux";
        }

        return name;
    }

    public static String normalizeArch(String rawArch) {
        String arch = rawArch.toLowerCase(Locale.ROOT);

        if (arch.contains("64") && (arch.contains("x86") || arch.contains("amd"))) {
            return "x64";
        }

        if (arch.contains("86") || arch.equals("i386") || arch.equals("i686")) {
            return "x86";
        }

        if (arch.contains("aarch64") || arch.contains("arm64")) {
            return "arm64";
        }

        if (arch.contains("arm")) {
            return "arm";
        }

        return arch;
    }
}