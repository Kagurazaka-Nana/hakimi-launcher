package com.minecraft.launcher.download;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 片表元数据 sidecar（{@code <目标>.part.meta}）：
 * 魔数 + 版本 + URL + total + ETag/Last-Modified + 分片表（含完成标记与分片级 CRC32C）。
 * 周期性落盘而非每个网络块都写盘；读取时任何结构异常都视为不存在（重新下载）。
 */
public final class PartMeta {

    private static final int MAGIC = 0x484B444C; // "HKDL"
    private static final int VERSION = 1;

    /** 单个分片的持久化记录。 */
    public record SegmentRecord(long start, long endInclusive, boolean done, long crc) {
        public Segment toSegment() {
            return new Segment(start, endInclusive);
        }
    }

    private final String url;
    private final long total;
    private final String etag;
    private final String lastModified;
    private final List<SegmentRecord> segments;

    public PartMeta(String url, long total, String etag, String lastModified, List<SegmentRecord> segments) {
        this.url = url;
        this.total = total;
        this.etag = etag;
        this.lastModified = lastModified;
        this.segments = List.copyOf(segments);
    }

    public String url() { return url; }
    public long total() { return total; }
    public String etag() { return etag; }
    public String lastModified() { return lastModified; }
    public List<SegmentRecord> segments() { return segments; }

    /** 与当前服务器探测结果是否一致（URL + 总长 + ETag/Last-Modified），一致才允许续传。 */
    public boolean matches(String url, long total, String etag, String lastModified) {
        return this.url.equals(url) && this.total == total
                && Objects.equals(this.etag, etag) && Objects.equals(this.lastModified, lastModified);
    }

    public List<Segment> pendingSegments() {
        List<Segment> out = new ArrayList<>();
        for (SegmentRecord s : segments) {
            if (!s.done()) {
                out.add(s.toSegment());
            }
        }
        return out;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PartMeta p)) return false;
        return total == p.total && Objects.equals(url, p.url) && Objects.equals(etag, p.etag)
                && Objects.equals(lastModified, p.lastModified) && Objects.equals(segments, p.segments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, total, etag, lastModified, segments);
    }

    public void write(Path file) throws IOException {
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
                Files.newOutputStream(tmp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)))) {
            out.writeInt(MAGIC);
            out.writeInt(VERSION);
            out.writeUTF(url);
            out.writeLong(total);
            out.writeBoolean(etag != null);
            if (etag != null) out.writeUTF(etag);
            out.writeBoolean(lastModified != null);
            if (lastModified != null) out.writeUTF(lastModified);
            out.writeInt(segments.size());
            for (SegmentRecord s : segments) {
                out.writeLong(s.start());
                out.writeLong(s.endInclusive());
                out.writeBoolean(s.done());
                out.writeLong(s.crc());
            }
        }
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        syncDir(file);
    }

    /** 结构异常/魔数不符一律返回 null（调用方按"无片表"处理）。 */
    public static PartMeta read(Path file) {
        if (!Files.exists(file)) {
            return null;
        }
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            if (in.readInt() != MAGIC) {
                return null;
            }
            if (in.readInt() != VERSION) {
                return null;
            }
            String url = in.readUTF();
            long total = in.readLong();
            String etag = in.readBoolean() ? in.readUTF() : null;
            String lastModified = in.readBoolean() ? in.readUTF() : null;
            int count = in.readInt();
            if (count < 0) {
                return null;
            }
            List<SegmentRecord> segments = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                long start = in.readLong();
                long end = in.readLong();
                boolean done = in.readBoolean();
                long crc = in.readLong();
                segments.add(new SegmentRecord(start, end, done, crc));
            }
            return new PartMeta(url, total, etag, lastModified, segments);
        } catch (IOException e) {
            return null;
        }
    }

    /** 元数据落盘后尝试 fsync 所在目录，降低掉电丢失风险（Windows 上可能不支持，忽略失败）。 */
    private static void syncDir(Path file) {
        try (FileChannel channel = FileChannel.open(file.getParent(), StandardOpenOption.READ)) {
            channel.force(true);
        } catch (Exception ignored) {
            // 目录 fsync 不保证所有平台支持
        }
    }
}
