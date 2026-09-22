package com.minecraft.launcher.download

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SpeedMeterTest {

    private var now = 0L

    @Test
    fun `empty meter reports zero`() {
        val meter = SpeedMeter(clock = { now })
        assertEquals(0, meter.bytesPerSec())
    }

    @Test
    fun `computes bytes per second over window`() {
        now = 0
        val meter = SpeedMeter(windowNanos = 10_000_000_000L, clock = { now })
        meter.add(1_000)   // t=0，累计 1000
        now = 2_000_000_000L
        meter.add(3_000)   // t=2s，累计 4000 → 窗口内增量 3000 / 2s = 1500B/s
        assertEquals(1_500, meter.bytesPerSec())
    }

    @Test
    fun `drops samples older than window`() {
        now = 0
        val meter = SpeedMeter(windowNanos = 1_000_000_000L, clock = { now })
        meter.add(5_000) // 累计 5000
        now = 5_000_000_000L
        meter.add(7_000) // 累计 12000；t=0 样本过窗被裁剪 → 仅剩一个样本
        assertEquals(0, meter.bytesPerSec())
        now = 6_000_000_000L
        meter.add(9_000) // 累计 21000；窗口 [5s=12000, 6s=21000] → 9000B/s
        assertEquals(9_000, meter.bytesPerSec())
    }

    @Test
    fun `monotonic non negative`() {
        now = 0
        val meter = SpeedMeter(clock = { now })
        meter.add(100)
        now = 1
        meter.add(50) // 累计值只增，不会出现负速率
        assertTrue(meter.bytesPerSec() >= 0)
    }
}
