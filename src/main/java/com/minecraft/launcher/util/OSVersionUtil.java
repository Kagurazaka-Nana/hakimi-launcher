package com.minecraft.launcher.util;

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.win32.StdCallLibrary;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("SpellCheckingInspection")
public class OSVersionUtil {

    // 1. 映射 Windows ntdll.dll 中的 RtlGetVersion 接口
    private interface NtDll extends StdCallLibrary {
        NtDll INSTANCE = Native.load("ntdll", NtDll.class);

        @Structure.FieldOrder({
                "dwOSVersionInfoSize",
                "dwMajorVersion",
                "dwMinorVersion",
                "dwBuildNumber",
                "dwPlatformId",
                "szCSDVersion"
        })
        class OSVERSIONINFOEX extends Structure {
            public int dwOSVersionInfoSize;
            public int dwMajorVersion;
            public int dwMinorVersion;
            public int dwBuildNumber;
            public int dwPlatformId;
            public char[] szCSDVersion = new char[128];

            OSVERSIONINFOEX() {
                super();
                // 注意：size() 必须在构造器中调用。
                // 若写成 "public int dwOSVersionInfoSize = size();" 字段初始化器，
                // 则此时后面的数组字段 szCSDVersion 尚未初始化，
                // JNA 的 deriveLayout 会抛 IllegalStateException("Array fields must be initialized")，
                // 导致 RtlGetVersion 调用失败并降级为 System.getProperty("os.version")。
                dwOSVersionInfoSize = size();
            }
        }

        // STATUS_SUCCESS 为 0
        int RtlGetVersion(OSVERSIONINFOEX result);
    }

    /**
     * 获取精确的 OS 版本字符串 (例如: "10.0.17134")
     */
    public static String getExactVersion() {
        // 非 Windows 系统直接返回 JVM 默认属性（如 Linux 的 kernel version）
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (!osName.contains("win")) {
            return System.getProperty("os.version");
        }

        try {
            NtDll.OSVERSIONINFOEX osvi = new NtDll.OSVERSIONINFOEX();
            int status = NtDll.INSTANCE.RtlGetVersion(osvi);

            if (status == 0) { // 0 代表 NTSTATUS SUCCESS
                return String.format("%d.%d.%d",
                        osvi.dwMajorVersion,
                        osvi.dwMinorVersion,
                        osvi.dwBuildNumber
                );
            }
        } catch (Throwable t) {
            // 兜底保护：防止某些奇葩裁剪版系统或 JNA 加载失败导致 Launcher 挂掉
        }

        // 如果调用失败，降级回 JVM 的默认属性
        return System.getProperty("os.version");
    }

    /**
     * 判断当前 Windows 构建号是否达到最低要求的版本
     */
    public static boolean isWindowsVersionAtLeast(int minMajor, int minMinor, int minBuild) {
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (!osName.contains("win")) return false;

        try {
            NtDll.OSVERSIONINFOEX osvi = new NtDll.OSVERSIONINFOEX();
            if (NtDll.INSTANCE.RtlGetVersion(osvi) == 0) {
                if (osvi.dwMajorVersion > minMajor) return true;
                if (osvi.dwMajorVersion < minMajor) return false;

                if (osvi.dwMinorVersion > minMinor) return true;
                if (osvi.dwMinorVersion < minMinor) return false;

                return osvi.dwBuildNumber >= minBuild;
            }
        } catch (Throwable ignored) {}

        return false;
    }

}