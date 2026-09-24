package com.minecraft.launcher.backend;

/** 系统运行监控指标（真实采样；速率由相邻两次采样差值换算）。 */
public final class SystemStats {

    private final int cpuPercent;
    private final double memUsedGb;
    private final double memTotalGb;
    /** 显存占用 MB；null = 本机无可用查询接口（UI 隐藏该项）。 */
    private final Long vramUsedMb;
    private final Long vramTotalMb;
    private final long netDownBps;
    private final long netUpBps;
    private final long diskReadBps;
    private final long diskWriteBps;

    public SystemStats(int cpuPercent, double memUsedGb, double memTotalGb,
                       Long vramUsedMb, Long vramTotalMb,
                       long netDownBps, long netUpBps, long diskReadBps, long diskWriteBps) {
        this.cpuPercent = cpuPercent;
        this.memUsedGb = memUsedGb;
        this.memTotalGb = memTotalGb;
        this.vramUsedMb = vramUsedMb;
        this.vramTotalMb = vramTotalMb;
        this.netDownBps = netDownBps;
        this.netUpBps = netUpBps;
        this.diskReadBps = diskReadBps;
        this.diskWriteBps = diskWriteBps;
    }

    public int getCpuPercent() { return cpuPercent; }
    public double getMemUsedGb() { return memUsedGb; }
    public double getMemTotalGb() { return memTotalGb; }
    public Long getVramUsedMb() { return vramUsedMb; }
    public Long getVramTotalMb() { return vramTotalMb; }
    public long getNetDownBps() { return netDownBps; }
    public long getNetUpBps() { return netUpBps; }
    public long getDiskReadBps() { return diskReadBps; }
    public long getDiskWriteBps() { return diskWriteBps; }

    @Override
    public String toString() {
        return "SystemStats(cpu=" + cpuPercent + "%, mem=" + memUsedGb + "/" + memTotalGb + "GB)";
    }
}
