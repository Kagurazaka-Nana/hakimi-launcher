package com.minecraft.launcher.download;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;

/**
 * 后端统一的文件下载入口接口：屏蔽具体下载引擎，业务侧只依赖本接口。
 *
 * 实现要求：URL 必须先经 SSRF 校验（仅 http/https，拒绝环回/私有/保留地址），
 * 校验失败抛 SecurityException。
 */
public interface FileDownloader extends Closeable {

    /** 异步下载：立即返回句柄，进度经 lastProgress 轮询，cancel 即暂停可续传。 */
    BitDownloader.DownloadJob download(String url, Path into);

    /**
     * 同步下载：阻塞当前线程直到完成，返回目标路径。
     * 失败抛原始异常（IOException 等）；供无虚拟线程上下文的调用方直接阻塞使用。
     */
    Path downloadBlocking(String url, Path into) throws IOException;

    /** 切换传输层 HTTP 代理：host 为 null/空 表示直连；仅影响后续新连接。 */
    void setProxy(String host, int port);

    /** 释放底层资源（取消全部在途下载）。 */
    @Override
    void close();
}
