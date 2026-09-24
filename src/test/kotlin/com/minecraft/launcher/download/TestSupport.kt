package com.minecraft.launcher.download

import java.time.Duration
import org.junit.jupiter.api.assertTimeoutPreemptively

/** 轮询等待条件成立（默认 30s 超时）。 */
fun awaitUntil(
    timeoutMs: Long = 30_000,
    intervalMs: Long = 50,
    describe: () -> String = { "" },
    predicate: () -> Boolean,
) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
        if (predicate()) return
        Thread.sleep(intervalMs)
    }
    throw AssertionError("条件超时未满足: ${describe()}")
}

/** 阻塞等待任务结束（带超时保护）。 */
fun BitDownloader.DownloadJob.awaitCompletionWithin(seconds: Long = 30) {
    assertTimeoutPreemptively(Duration.ofSeconds(seconds)) { awaitCompletion() }
}
