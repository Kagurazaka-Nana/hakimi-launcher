package com.minecraft.launcher.download;

import java.util.concurrent.locks.LockSupport;

/**
 * 令牌桶限速器：以恒定速率补充令牌，acquire 按字节数扣减并阻塞等待（虚拟线程友好）。
 * maxBytesPerSec <= 0 表示不限速，acquire 直接返回。
 */
public final class TokenBucketRateLimiter {

    private final long maxBytesPerSec;
    private double tokens;
    private long lastRefillNanos;

    public TokenBucketRateLimiter(long maxBytesPerSec) {
        this.maxBytesPerSec = maxBytesPerSec;
        this.tokens = maxBytesPerSec;
        this.lastRefillNanos = System.nanoTime();
    }

    /** 扣减 bytes 个令牌；不足时阻塞当前（虚拟）线程等待补充。 */
    public void acquire(int bytes) throws InterruptedException {
        if (maxBytesPerSec <= 0) {
            return;
        }
        while (true) {
            long waitNanos;
            synchronized (this) {
                refill();
                if (tokens >= bytes) {
                    tokens -= bytes;
                    return;
                }
                waitNanos = (long) Math.max(1_000_000L, (bytes - tokens) / maxBytesPerSec * 1e9);
            }
            LockSupport.parkNanos(waitNanos);
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("rate limit wait interrupted");
            }
        }
    }

    private void refill() {
        long now = System.nanoTime();
        long elapsed = now - lastRefillNanos;
        if (elapsed > 0) {
            tokens = Math.min(maxBytesPerSec, tokens + elapsed / 1e9 * maxBytesPerSec);
            lastRefillNanos = now;
        }
    }
}
