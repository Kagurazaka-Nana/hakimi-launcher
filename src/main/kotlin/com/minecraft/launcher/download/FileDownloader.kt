package com.minecraft.launcher.download

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import java.nio.file.Path

/**
 * 后端统一的文件下载入口接口：屏蔽具体下载引擎，业务侧只依赖本接口。
 *
 * 实现要求：URL 必须先经 SSRF 校验（仅 http/https，拒绝环回/私有/保留地址），
 * 校验失败抛 [SecurityException]。
 */
interface FileDownloader : AutoCloseable {

    /** 异步下载：立即返回句柄，进度经 [BitDownloader.DownloadJob.progress] 消费，cancel 即暂停可续传。 */
    fun download(url: String, into: Path): BitDownloader.DownloadJob

    /** 同步下载：阻塞当前线程直到完成，返回目标路径。
     * 失败抛原始异常（IOException 等）；供 Java 侧（CLI/后端编排）直接调用。 */
    fun downloadBlocking(url: String, into: Path): Path

    /** 切换传输层 HTTP 代理：host 为 null/空 表示直连；仅影响后续新连接。 */
    fun setProxy(host: String?, port: Int)

    /** 释放底层协程作用域与连接资源。 */
    override fun close()
}

/**
 * [FileDownloader] 的默认实现：委托 [BitDownloader]（虚拟线程 + 协程 + 动态分片）。
 * 自带作用域（IO 调度器），close 时取消全部在途下载。
 */
class BitFileDownloader(
    private val config: DownloadConfig = DownloadConfig(),
    /** SSRF 校验函数；测试注入本地回环服务器时替换为恒通过实现。 */
    urlGuard: (String) -> java.net.URI = UrlGuard::validate,
) : FileDownloader {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val downloader = BitDownloader(scope, config, urlGuard)

    override fun download(url: String, into: Path): BitDownloader.DownloadJob =
        downloader.download(url, into)

    override fun downloadBlocking(url: String, into: Path): Path = runBlocking {
        val job = downloader.download(url, into)
        job.awaitCompletion()
        into
    }

    override fun setProxy(host: String?, port: Int) = downloader.setProxy(host, port)

    override fun close() {
        scope.cancel()
    }
}
