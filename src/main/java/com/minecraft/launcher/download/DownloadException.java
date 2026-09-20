package com.minecraft.launcher.download;

/** 下载过程中的运行时异常。 */
public class DownloadException extends RuntimeException {

    public DownloadException(String message, Throwable cause) {
        super(message, cause);
    }

    public DownloadException(String message) {
        super(message);
    }
}
