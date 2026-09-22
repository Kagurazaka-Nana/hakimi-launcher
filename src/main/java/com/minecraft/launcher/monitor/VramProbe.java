package com.minecraft.launcher.monitor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 显存探测（尽力而为）：目前仅 NVIDIA 的 nvidia-smi 能提供系统级显存占用。
 *
 * <p>其它途径为何不可用：Windows 的 GPU 性能计数器依赖驱动注册（部分驱动如国产 GPU 未注册，
 * WMI {@code Win32_VideoController.AdapterRAM} 也可能为 0 或按 32 位截断）；
 * DXGI {@code QueryVideoMemoryInfo} 只能反映调用进程自己的显存配额，不符合“系统级状态栏”语义。
 * 因此不可得时返回 null，由 UI 隐藏显存项。
 */
public final class VramProbe {

    public record Vram(long usedMb, long totalMb) {}

    /** nvidia-smi 单次约 100~300ms，结果缓存 3 秒，避免每秒拉起进程。 */
    private static final long CACHE_NANOS = 3_000_000_000L;

    private Vram cached;
    private long cachedAtNanos;
    private boolean probedUnavailable;

    public Vram query() {
        if (probedUnavailable) {
            return null;
        }
        long now = System.nanoTime();
        if (cached != null && now - cachedAtNanos < CACHE_NANOS) {
            return cached;
        }
        Vram v = queryNvidiaSmi();
        if (v == null) {
            if (cached == null) {
                probedUnavailable = true;
            }
            return null;
        }
        cached = v;
        cachedAtNanos = now;
        return v;
    }

    private Vram queryNvidiaSmi() {
        Process p = null;
        try {
            p = new ProcessBuilder("nvidia-smi", "--query-gpu=memory.used,memory.total", "--format=csv,noheader,nounits")
                    .redirectErrorStream(true)
                    .start();
            String line;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                line = r.readLine();
            }
            if (!p.waitFor(2, TimeUnit.SECONDS) || p.exitValue() != 0 || line == null) {
                return null;
            }
            String[] parts = line.split(",");
            if (parts.length < 2) {
                return null;
            }
            long used = Long.parseLong(parts[0].trim());
            long total = Long.parseLong(parts[1].trim());
            return total > 0 ? new Vram(used, total) : null;
        } catch (Exception e) {
            return null;
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
    }
}
