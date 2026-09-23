package com.minecraft.launcher.download

import com.minecraft.launcher.backend.DownloadTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import java.nio.file.Path
import java.util.UUID

/**
 * 下载任务编排层：接收 UI 的下载事件（start/cancel/setProxy），委托 [FileDownloader] 执行，
 * 并把每个任务的进度流汇总成单一 [tasksFlow]，供底部指示器与下载弹窗消费。
 *
 * 任务到达终态（完成/取消/失败）后不再移除，而是标记状态作为历史保留（最多 [MAX_HISTORY] 条，
 * 新任务在前）；指示器据此只统计活跃任务，弹窗展示全部。
 * URL 的 SSRF 校验由下载器在 [start] 时同步执行（不合法直接抛 [SecurityException]，任务不入队）。
 */
class DownloadManager(
    private val downloader: FileDownloader = BitFileDownloader(),
) : AutoCloseable {

    private companion object {
        const val MAX_HISTORY = 50
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Any()
    private val jobs = HashMap<String, BitDownloader.DownloadJob>()

    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())

    /** 当前下载任务列表快照流（活跃 + 历史，新任务在前）。 */
    fun tasksFlow(): StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    /** 发起下载事件：返回任务 id；URL 校验失败同步抛 [SecurityException]。 */
    fun start(url: String, into: Path): String {
        val job = downloader.download(url, into)
        val id = UUID.randomUUID().toString()
        val name = into.fileName.toString()
        synchronized(lock) {
            jobs[id] = job
            _tasks.value = (
                listOf(DownloadTask(id, name, url, 0f, DownloadState.CONNECTING)) + _tasks.value
                ).take(MAX_HISTORY)
        }
        scope.launch {
            job.progress
                .takeWhile { it.state != DownloadState.COMPLETED && it.state != DownloadState.CANCELLED && it.state != DownloadState.FAILED }
                .collect { p ->
                    val fraction = if (p.totalBytes > 0) (p.downloadedBytes.toFloat() / p.totalBytes).coerceIn(0f, 1f) else 0f
                    updateFraction(id, fraction)
                }
            // 终态：replay=1 的最后一个值即终态进度
            val terminal = runCatching { job.progress.first() }.getOrNull()
            val terminalFraction = terminal?.let { if (it.totalBytes > 0) (it.downloadedBytes.toFloat() / it.totalBytes).coerceIn(0f, 1f) else 0f } ?: 0f
            markTerminal(id, terminal?.state ?: DownloadState.FAILED, terminalFraction)
        }
        return id
    }

    /** 取消（暂停）任务：保留 .part，可再次 start 同 URL 续传。 */
    fun cancel(id: String) {
        synchronized(lock) { jobs[id] }?.cancel()
    }

    /**
     * 外部编排任务句柄：安装这类「多文件聚合」操作没有单一下载任务可挂，
     * 用本句柄把聚合进度推入同一队列（指示器/弹窗统一展示）。
     */
    class ExternalTask internal constructor(
        val id: String,
        private val onUpdate: (Float) -> Unit,
        private val onTerminal: (DownloadState) -> Unit,
    ) {
        fun update(fraction: Float) = onUpdate(fraction)
        fun complete() = onTerminal(DownloadState.COMPLETED)
        fun fail() = onTerminal(DownloadState.FAILED)
    }

    /** 登记一个外部编排任务（初始为下载中），返回进度句柄。 */
    fun trackExternal(name: String, url: String): ExternalTask {
        val id = UUID.randomUUID().toString()
        synchronized(lock) {
            _tasks.value = (
                listOf(DownloadTask(id, name, url, 0f, DownloadState.DOWNLOADING)) + _tasks.value
                ).take(MAX_HISTORY)
        }
        return ExternalTask(
            id,
            onUpdate = { f ->
                synchronized(lock) {
                    _tasks.value = _tasks.value.map {
                        if (it.id == id && it.state == DownloadState.DOWNLOADING) it.copy(fraction = f.coerceIn(0f, 1f)) else it
                    }
                }
            },
            onTerminal = { st ->
                synchronized(lock) {
                    _tasks.value = _tasks.value.map {
                        if (it.id == id) {
                            it.copy(state = st, fraction = if (st == DownloadState.COMPLETED) 1f else it.fraction)
                        } else it
                    }
                }
            },
        )
    }

    /** 切换传输层代理（host 为 null/空 表示直连）。 */
    fun setProxy(host: String?, port: Int) = downloader.setProxy(host, port)

    private fun updateFraction(id: String, fraction: Float) {
        synchronized(lock) {
            _tasks.value = _tasks.value.map {
                if (it.id == id && it.state == DownloadState.CONNECTING) it.copy(fraction = fraction, state = DownloadState.DOWNLOADING)
                else if (it.id == id && it.state == DownloadState.DOWNLOADING) it.copy(fraction = fraction)
                else it
            }
        }
    }

    private fun markTerminal(id: String, state: DownloadState, fraction: Float) {
        synchronized(lock) {
            jobs.remove(id)
            _tasks.value = _tasks.value.map {
                if (it.id == id) it.copy(state = state, fraction = if (state == DownloadState.COMPLETED) 1f else fraction) else it
            }
        }
    }

    override fun close() {
        scope.cancel()
        downloader.close()
    }
}
