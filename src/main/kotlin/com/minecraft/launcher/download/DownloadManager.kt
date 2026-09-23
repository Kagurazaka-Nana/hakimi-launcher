package com.minecraft.launcher.download

import com.minecraft.launcher.backend.DownloadTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import java.nio.file.Path
import java.util.UUID

/**
 * 下载任务编排层：接收 UI 的下载事件（start/cancel），委托 [FileDownloader] 执行，
 * 并把每个任务的进度流汇总成单一 [tasksFlow]，供底部"下载内容"指示器消费。
 *
 * 任务在到达终态（完成/取消/失败）后自动移出队列；URL 的 SSRF 校验由下载器在
 * [start] 时同步执行（不合法直接抛 [SecurityException]，任务不会入队）。
 */
class DownloadManager(
    private val downloader: FileDownloader = BitFileDownloader(),
) : AutoCloseable {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Any()
    private val jobs = HashMap<String, BitDownloader.DownloadJob>()

    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())

    /** 当前下载队列快照流（任务完成/取消/失败后自动移除）。 */
    fun tasksFlow(): StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    /** 发起下载事件：返回任务 id；URL 校验失败同步抛 [SecurityException]。 */
    fun start(url: String, into: Path): String {
        val job = downloader.download(url, into)
        val id = UUID.randomUUID().toString()
        val name = into.fileName.toString()
        synchronized(lock) {
            jobs[id] = job
            _tasks.value = _tasks.value + DownloadTask(id, name, 0f)
        }
        scope.launch {
            // SharedFlow 不会自行结束：收到终态即退出并移除任务
            job.progress
                .takeWhile { it.state != DownloadState.COMPLETED && it.state != DownloadState.CANCELLED && it.state != DownloadState.FAILED }
                .collect { p ->
                    val fraction = if (p.totalBytes > 0) (p.downloadedBytes.toFloat() / p.totalBytes).coerceIn(0f, 1f) else 0f
                    updateFraction(id, fraction)
                }
            remove(id)
        }
        return id
    }

    /** 取消（暂停）任务：保留 .part，可再次 start 同 URL 续传。 */
    fun cancel(id: String) {
        synchronized(lock) { jobs[id] }?.cancel()
    }

    private fun updateFraction(id: String, fraction: Float) {
        synchronized(lock) {
            if (!jobs.containsKey(id)) return
            _tasks.value = _tasks.value.map { if (it.id == id) it.copy(fraction = fraction) else it }
        }
    }

    private fun remove(id: String) {
        synchronized(lock) {
            jobs.remove(id)
            _tasks.value = _tasks.value.filterNot { it.id == id }
        }
    }

    override fun close() {
        scope.cancel()
        downloader.close()
    }
}
