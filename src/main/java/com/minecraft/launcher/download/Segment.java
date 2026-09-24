package com.minecraft.launcher.download;

/**
 * 一个待传输区间 [start, endInclusive]；endInclusive = -1 表示未知长度（读到 EOF）。
 * 动态分片调度的工作单元：worker 从队列领取，谁快谁多取；跑得慢的片把剩余区间对半切推回队列。
 */
public record Segment(long start, long endInclusive) {

    public long size() {
        return endInclusive < 0 ? -1 : endInclusive - start + 1;
    }
}
