package com.minecraft.launcher.download;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.LongSupplier;

/**
 * 滑动窗口测速：维护窗口内的 (时间戳, 累计字节) 采样，速率 = 窗口首尾字节差 / 时间差。
 */
public final class SpeedMeter {

    private final long windowNanos;
    private final LongSupplier clock;
    private final Deque<long[]> samples = new ArrayDeque<>();
    private long cumulative;

    public SpeedMeter() {
        this(5_000_000_000L, System::nanoTime);
    }

    public SpeedMeter(long windowNanos, LongSupplier clock) {
        this.windowNanos = windowNanos;
        this.clock = clock;
    }

    public synchronized void add(long bytes) {
        cumulative += bytes;
        long now = clock.getAsLong();
        samples.addLast(new long[]{now, cumulative});
        trim(now);
    }

    /** 窗口内的平均速率（字节/秒）；样本不足两个时返回 0。 */
    public synchronized long bytesPerSec() {
        long now = clock.getAsLong();
        trim(now);
        if (samples.size() < 2) {
            return 0;
        }
        long[] first = samples.peekFirst();
        long[] last = samples.peekLast();
        long dt = last[0] - first[0];
        if (dt <= 0) {
            return 0;
        }
        return Math.max(0, (long) ((last[1] - first[1]) / (double) dt * 1e9));
    }

    private void trim(long now) {
        while (samples.size() > 1 && now - samples.peekFirst()[0] > windowNanos) {
            samples.pollFirst();
        }
    }
}
