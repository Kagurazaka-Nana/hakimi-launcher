package com.minecraft.launcher.download;

import java.time.Duration;

/** 下载器配置（不可变；builder 提供默认值与校验）。 */
public final class DownloadConfig {

    /** 单任务并发连接数：8~16 是甜点区，再多通常触发服务端限流。 */
    private final int connections;
    /** 分片最小体积（字节）：动态切分不会低于该值。 */
    private final long minSegmentSize;
    /** 初始分片数 = connections * initialSegmentsPerConnection。 */
    private final int initialSegmentsPerConnection;
    /** 单次网络读的缓冲大小（字节）。 */
    private final int bufferSize;
    /** 限速（字节/秒），0 = 不限速。 */
    private final long maxBytesPerSec;
    /** HTTP 代理（传输层）；null = 直连。SSRF 校验仍针对最终请求 URL。 */
    private final String proxyHost;
    private final int proxyPort;
    private final Duration connectTimeout;
    /** 片表元数据定时落盘间隔（不对每个网络块都写盘）。 */
    private final Duration metaFlushInterval;
    /** 进度发射间隔。 */
    private final Duration progressInterval;

    private DownloadConfig(Builder b) {
        if (b.connections < 1 || b.connections > 64) throw new IllegalArgumentException("connections 应在 1..64");
        if (b.minSegmentSize <= 0) throw new IllegalArgumentException("minSegmentSize 必须为正");
        if (b.bufferSize < 8192) throw new IllegalArgumentException("bufferSize 至少 8KB");
        if (b.maxBytesPerSec < 0) throw new IllegalArgumentException("maxBytesPerSec 不能为负");
        if (b.proxyHost != null && (b.proxyPort < 1 || b.proxyPort > 65535)) {
            throw new IllegalArgumentException("设置代理时端口必须有效");
        }
        this.connections = b.connections;
        this.minSegmentSize = b.minSegmentSize;
        this.initialSegmentsPerConnection = b.initialSegmentsPerConnection;
        this.bufferSize = b.bufferSize;
        this.maxBytesPerSec = b.maxBytesPerSec;
        this.proxyHost = b.proxyHost;
        this.proxyPort = b.proxyPort;
        this.connectTimeout = b.connectTimeout;
        this.metaFlushInterval = b.metaFlushInterval;
        this.progressInterval = b.progressInterval;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static DownloadConfig defaults() {
        return builder().build();
    }

    public int getConnections() { return connections; }
    public long getMinSegmentSize() { return minSegmentSize; }
    public int getInitialSegmentsPerConnection() { return initialSegmentsPerConnection; }
    public int getBufferSize() { return bufferSize; }
    public long getMaxBytesPerSec() { return maxBytesPerSec; }
    public String getProxyHost() { return proxyHost; }
    public int getProxyPort() { return proxyPort; }
    public Duration getConnectTimeout() { return connectTimeout; }
    public Duration getMetaFlushInterval() { return metaFlushInterval; }
    public Duration getProgressInterval() { return progressInterval; }

    public static final class Builder {
        private int connections = 8;
        private long minSegmentSize = 1L << 20;
        private int initialSegmentsPerConnection = 4;
        private int bufferSize = 256 << 10;
        private long maxBytesPerSec = 0;
        private String proxyHost = null;
        private int proxyPort = 0;
        private Duration connectTimeout = Duration.ofSeconds(15);
        private Duration metaFlushInterval = Duration.ofSeconds(2);
        private Duration progressInterval = Duration.ofMillis(300);

        public Builder connections(int v) { this.connections = v; return this; }
        public Builder minSegmentSize(long v) { this.minSegmentSize = v; return this; }
        public Builder initialSegmentsPerConnection(int v) { this.initialSegmentsPerConnection = v; return this; }
        public Builder bufferSize(int v) { this.bufferSize = v; return this; }
        public Builder maxBytesPerSec(long v) { this.maxBytesPerSec = v; return this; }
        public Builder proxyHost(String v) { this.proxyHost = v; return this; }
        public Builder proxyPort(int v) { this.proxyPort = v; return this; }
        public Builder connectTimeout(Duration v) { this.connectTimeout = v; return this; }
        public Builder metaFlushInterval(Duration v) { this.metaFlushInterval = v; return this; }
        public Builder progressInterval(Duration v) { this.progressInterval = v; return this; }

        public DownloadConfig build() {
            return new DownloadConfig(this);
        }
    }
}
