package com.minecraft.launcher.download

/**
 * 滑动窗口测速：维护窗口内的 (时间戳, 累计字节) 采样，速率 = 窗口首尾字节差 / 时间差。
 */
class SpeedMeter(
    private val windowNanos: Long = 5_000_000_000L,
    private val clock: () -> Long = System::nanoTime,
) {

    private val samples = ArrayDeque<Pair<Long, Long>>()
    private var cumulative: Long = 0

    @Synchronized
    fun add(bytes: Long) {
        cumulative += bytes
        val now = clock()
        samples.addLast(now to cumulative)
        trim(now)
    }

    /** 窗口内的平均速率（字节/秒）；样本不足两个时返回 0。 */
    @Synchronized
    fun bytesPerSec(): Long {
        val now = clock()
        trim(now)
        if (samples.size < 2) return 0
        val (t0, b0) = samples.first()
        val (t1, b1) = samples.last()
        val dt = t1 - t0
        if (dt <= 0) return 0
        return ((b1 - b0).toDouble() / dt * 1e9).toLong().coerceAtLeast(0)
    }

    private fun trim(now: Long) {
        while (samples.size > 1 && now - samples.first().first > windowNanos) {
            samples.removeFirst()
        }
    }
}
