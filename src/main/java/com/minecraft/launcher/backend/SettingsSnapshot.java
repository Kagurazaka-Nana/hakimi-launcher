package com.minecraft.launcher.backend;

/** 设置页快照。 */
public final class SettingsSnapshot {

    private final String theme;
    private final String language;
    private final String javaPath;
    private final String javaVersion;
    private final int maxMemoryMb;
    private final int memoryMinMb;
    private final int memoryMaxMb;
    private final String downloadSource;
    private final int concurrency;
    private final int concurrencyMin;
    private final int concurrencyMax;
    private final String jvmArgs;
    private final boolean debugMode;
    private final String account;
    private final String privacy;
    /** 网络代理（传输层）；host 为空或 enabled=false 表示直连。 */
    private final boolean proxyEnabled;
    private final String proxyHost;
    private final int proxyPort;

    private SettingsSnapshot(Builder b) {
        theme = b.theme;
        language = b.language;
        javaPath = b.javaPath;
        javaVersion = b.javaVersion;
        maxMemoryMb = b.maxMemoryMb;
        memoryMinMb = b.memoryMinMb;
        memoryMaxMb = b.memoryMaxMb;
        downloadSource = b.downloadSource;
        concurrency = b.concurrency;
        concurrencyMin = b.concurrencyMin;
        concurrencyMax = b.concurrencyMax;
        jvmArgs = b.jvmArgs;
        debugMode = b.debugMode;
        account = b.account;
        privacy = b.privacy;
        proxyEnabled = b.proxyEnabled;
        proxyHost = b.proxyHost;
        proxyPort = b.proxyPort;
    }

    public String getTheme() { return theme; }
    public String getLanguage() { return language; }
    public String getJavaPath() { return javaPath; }
    public String getJavaVersion() { return javaVersion; }
    public int getMaxMemoryMb() { return maxMemoryMb; }
    public int getMemoryMinMb() { return memoryMinMb; }
    public int getMemoryMaxMb() { return memoryMaxMb; }
    public String getDownloadSource() { return downloadSource; }
    public int getConcurrency() { return concurrency; }
    public int getConcurrencyMin() { return concurrencyMin; }
    public int getConcurrencyMax() { return concurrencyMax; }
    public String getJvmArgs() { return jvmArgs; }
    public boolean getDebugMode() { return debugMode; }
    public String getAccount() { return account; }
    public String getPrivacy() { return privacy; }
    public boolean getProxyEnabled() { return proxyEnabled; }
    public String getProxyHost() { return proxyHost; }
    public int getProxyPort() { return proxyPort; }

    public Builder toBuilder() {
        return new Builder()
                .theme(theme).language(language).javaPath(javaPath).javaVersion(javaVersion)
                .maxMemoryMb(maxMemoryMb).memoryMinMb(memoryMinMb).memoryMaxMb(memoryMaxMb)
                .downloadSource(downloadSource).concurrency(concurrency)
                .concurrencyMin(concurrencyMin).concurrencyMax(concurrencyMax)
                .jvmArgs(jvmArgs).debugMode(debugMode).account(account).privacy(privacy)
                .proxyEnabled(proxyEnabled).proxyHost(proxyHost).proxyPort(proxyPort);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String theme;
        private String language;
        private String javaPath;
        private String javaVersion;
        private int maxMemoryMb;
        private int memoryMinMb;
        private int memoryMaxMb;
        private String downloadSource;
        private int concurrency;
        private int concurrencyMin;
        private int concurrencyMax;
        private String jvmArgs;
        private boolean debugMode;
        private String account;
        private String privacy;
        private boolean proxyEnabled;
        private String proxyHost = "";
        private int proxyPort;

        public Builder theme(String v) { theme = v; return this; }
        public Builder language(String v) { language = v; return this; }
        public Builder javaPath(String v) { javaPath = v; return this; }
        public Builder javaVersion(String v) { javaVersion = v; return this; }
        public Builder maxMemoryMb(int v) { maxMemoryMb = v; return this; }
        public Builder memoryMinMb(int v) { memoryMinMb = v; return this; }
        public Builder memoryMaxMb(int v) { memoryMaxMb = v; return this; }
        public Builder downloadSource(String v) { downloadSource = v; return this; }
        public Builder concurrency(int v) { concurrency = v; return this; }
        public Builder concurrencyMin(int v) { concurrencyMin = v; return this; }
        public Builder concurrencyMax(int v) { concurrencyMax = v; return this; }
        public Builder jvmArgs(String v) { jvmArgs = v; return this; }
        public Builder debugMode(boolean v) { debugMode = v; return this; }
        public Builder account(String v) { account = v; return this; }
        public Builder privacy(String v) { privacy = v; return this; }
        public Builder proxyEnabled(boolean v) { proxyEnabled = v; return this; }
        public Builder proxyHost(String v) { proxyHost = v; return this; }
        public Builder proxyPort(int v) { proxyPort = v; return this; }

        public SettingsSnapshot build() {
            return new SettingsSnapshot(this);
        }
    }
}
