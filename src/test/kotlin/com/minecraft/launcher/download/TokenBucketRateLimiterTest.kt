package com.minecraft.launcher.download

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TokenBucketRateLimiterTest {

    @Test
    fun `unlimited acquires immediately`() = runBlocking {
        val limiter = TokenBucketRateLimiter(0)
        val start = System.nanoTime()
        repeat(100) { limiter.acquire(1 shl 20) }
        assertTrue(System.nanoTime() - start < 100_000_000L, "不限速不应挂起")
    }

    @Test
    fun `rate limits to configured bytes per sec`() = runBlocking {
        val limiter = TokenBucketRateLimiter(100_000) // 100KB/s
        val bytes = 250_000L
        val start = System.nanoTime()
        withTimeout(10_000) {
            var remaining = bytes
            while (remaining > 0) {
                val chunk = minOf(remaining, 50_000).toInt()
                limiter.acquire(chunk)
                remaining -= chunk
            }
        }
        val elapsedSec = (System.nanoTime() - start) / 1e9
        // 桶初始有 100KB 余量，实际传输 250KB，理想耗时约 (250-100)/100 = 1.5s；放宽下界防抖动
        assertTrue(elapsedSec > 0.8, "应被限速拖慢，实际 ${"%.2f".format(elapsedSec)}s")
        assertTrue(elapsedSec < 5, "不应过度挂起，实际 ${"%.2f".format(elapsedSec)}s")
    }

    @Test
    fun `concurrent acquires share the same bucket`() = runBlocking {
        val limiter = TokenBucketRateLimiter(200_000)
        val start = System.nanoTime()
        withTimeout(10_000) {
            (1..4).map { async(Dispatchers.IO) { repeat(5) { limiter.acquire(20_000) } } }.awaitAll()
        }
        val elapsedSec = (System.nanoTime() - start) / 1e9
        // 总量 400KB，桶容量 200KB，理想 (400-200)/200 = 1s
        assertTrue(elapsedSec > 0.3, "并发应共享令牌，实际 ${"%.2f".format(elapsedSec)}s")
        assertEquals(4 * 5 * 20_000, 400_000)
    }
}
