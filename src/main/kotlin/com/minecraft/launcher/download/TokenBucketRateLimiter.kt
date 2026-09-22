package com.minecraft.launcher.download

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 令牌桶限速器：以恒定速率补充令牌，acquire 按字节数扣减并按需挂起。
 * maxBytesPerSec <= 0 表示不限速，acquire 直接返回。
 */
class TokenBucketRateLimiter(private val maxBytesPerSec: Long) {

    private val mutex = Mutex()
    private var tokens: Double = maxBytesPerSec.toDouble()
    private var lastRefillNanos: Long = System.nanoTime()

    /** 扣减 [bytes] 个令牌；不足时挂起等待补充。 */
    suspend fun acquire(bytes: Int) {
        if (maxBytesPerSec <= 0) return
        while (true) {
            val waitMillis = mutex.withLock {
                refill()
                if (tokens >= bytes) {
                    tokens -= bytes
                    0L
                } else {
                    ((bytes - tokens) / maxBytesPerSec * 1000.0).coerceAtLeast(1.0).toLong()
                }
            }
            if (waitMillis == 0L) return
            delay(waitMillis)
        }
    }

    private fun refill() {
        val now = System.nanoTime()
        val elapsed = now - lastRefillNanos
        if (elapsed <= 0) return
        tokens = (tokens + elapsed / 1e9 * maxBytesPerSec).coerceAtMost(maxBytesPerSec.toDouble())
        lastRefillNanos = now
    }
}
