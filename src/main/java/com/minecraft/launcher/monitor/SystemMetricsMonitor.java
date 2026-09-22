package com.minecraft.launcher.monitor;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.NetworkIF;

/**
 * 真实系统指标采集：CPU 占用 / 物理内存 / 网卡收发 / 磁盘读写累计量（OSHI），
 * 速率由相邻两次采样的差值换算；显存走 {@link VramProbe} 尽力探测（null = 不可得）。
 *
 * <p>非线程安全：{@link #sample()} 需由调用方串行执行（当前唯一调用方是后端的单条采集协程）。
 */
public final class SystemMetricsMonitor {

    /** 一次采样结果；显存字段在无可用查询接口的机器上为 null。 */
    public record Snapshot(
            int cpuPercent,
            double memUsedGb,
            double memTotalGb,
            Long vramUsedMb,
            Long vramTotalMb,
            long netDownBps,
            long netUpBps,
            long diskReadBps,
            long diskWriteBps) {}

    private static final double BYTES_PER_GB = 1_000_000_000.0;
    /** IFTYPE_SOFTWARE_LOOPBACK（RFC 2863），OSHI 常量在不同版本命名不稳定，直接按值过滤。 */
    private static final int IF_TYPE_SOFTWARE_LOOPBACK = 24;

    private final SystemInfo system = new SystemInfo();
    private final CentralProcessor processor = system.getHardware().getProcessor();
    private final VramProbe vramProbe = new VramProbe();

    private long[] prevTicks;
    private long prevRecv;
    private long prevSent;
    private long prevRead;
    private long prevWrite;
    private long prevNanos;

    /**
     * 采样当前指标。首次调用时内部取 250ms 基线计算 CPU（速率字段为 0），
     * 之后每次与上一次采样求差。
     */
    public Snapshot sample() {
        double cpuLoad;
        if (prevTicks == null) {
            // 首次调用：取 250ms 短基线，避免第一帧 CPU 为 0
            prevTicks = processor.getSystemCpuLoadTicks();
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks);
        prevTicks = processor.getSystemCpuLoadTicks();

        GlobalMemory memory = system.getHardware().getMemory();
        long recv = 0;
        long sent = 0;
        for (NetworkIF nif : system.getHardware().getNetworkIFs()) {
            nif.updateAttributes();
            if (nif.getIfType() == IF_TYPE_SOFTWARE_LOOPBACK) {
                continue;
            }
            recv += Math.max(nif.getBytesRecv(), 0);
            sent += Math.max(nif.getBytesSent(), 0);
        }
        long read = 0;
        long write = 0;
        for (HWDiskStore disk : system.getHardware().getDiskStores()) {
            disk.updateAttributes();
            read += Math.max(disk.getReadBytes(), 0);
            write += Math.max(disk.getWriteBytes(), 0);
        }

        long now = System.nanoTime();
        double seconds = prevNanos == 0 ? 0 : (now - prevNanos) / 1_000_000_000.0;
        long netDown = seconds > 0 ? (long) ((recv - prevRecv) / seconds) : 0;
        long netUp = seconds > 0 ? (long) ((sent - prevSent) / seconds) : 0;
        long diskRead = seconds > 0 ? (long) ((read - prevRead) / seconds) : 0;
        long diskWrite = seconds > 0 ? (long) ((write - prevWrite) / seconds) : 0;

        prevRecv = recv;
        prevSent = sent;
        prevRead = read;
        prevWrite = write;
        prevNanos = now;

        VramProbe.Vram vram = vramProbe.query();
        return new Snapshot(
                (int) Math.round(cpuLoad * 100),
                (memory.getTotal() - memory.getAvailable()) / BYTES_PER_GB,
                memory.getTotal() / BYTES_PER_GB,
                vram == null ? null : vram.usedMb(),
                vram == null ? null : vram.totalMb(),
                Math.max(netDown, 0),
                Math.max(netUp, 0),
                Math.max(diskRead, 0),
                Math.max(diskWrite, 0));
    }
}
