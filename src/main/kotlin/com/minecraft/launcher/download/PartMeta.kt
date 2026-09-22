package com.minecraft.launcher.download

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

/**
 * 片表元数据 sidecar（`<目标>.part.meta`）：
 * 魔数 + 版本 + URL + total + ETag/Last-Modified + 分片表（含完成标记与分片级 CRC32C）。
 * 周期性落盘而非每个网络块都写盘；读取时任何结构异常都视为不存在（重新下载）。
 */
data class PartMeta(
    val url: String,
    val total: Long,
    val etag: String?,
    val lastModified: String?,
    val segments: List<SegmentRecord>,
) {
    /** 单个分片的持久化记录。 */
    data class SegmentRecord(
        val start: Long,
        val endInclusive: Long,
        val done: Boolean,
        /** 分片内容 CRC32C；未完成时无意义。 */
        val crc: Long = 0,
    ) {
        fun toSegment() = Segment(start, endInclusive)
    }

    /** 与当前服务器探测结果是否一致（URL + 总长 + ETag/Last-Modified），一致才允许续传。 */
    fun matches(url: String, total: Long, etag: String?, lastModified: String?): Boolean =
        this.url == url && this.total == total && this.etag == etag && this.lastModified == lastModified

    fun pendingSegments(): List<Segment> = segments.filter { !it.done }.map { it.toSegment() }

    fun write(file: Path) {
        val tmp = file.resolveSibling(file.fileName.toString() + ".tmp")
        DataOutputStream(
            BufferedOutputStream(Files.newOutputStream(tmp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)),
        ).use { out ->
            out.writeInt(MAGIC)
            out.writeInt(VERSION)
            out.writeUTF(url)
            out.writeLong(total)
            out.writeBoolean(etag != null)
            if (etag != null) out.writeUTF(etag)
            out.writeBoolean(lastModified != null)
            if (lastModified != null) out.writeUTF(lastModified)
            out.writeInt(segments.size)
            segments.forEach { s ->
                out.writeLong(s.start)
                out.writeLong(s.endInclusive)
                out.writeBoolean(s.done)
                out.writeLong(s.crc)
            }
        }
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        syncDir(file)
    }

    companion object {
        private const val MAGIC = 0x484B444C // "HKDL"
        private const val VERSION = 1

        fun read(file: Path): PartMeta? {
            if (!Files.exists(file)) return null
            return runCatching {
                DataInputStream(BufferedInputStream(Files.newInputStream(file))).use { input ->
                    val magic = input.readInt()
                    if (magic != MAGIC) return null
                    val version = input.readInt()
                    if (version != VERSION) return null
                    val url = input.readUTF()
                    val total = input.readLong()
                    val etag = if (input.readBoolean()) input.readUTF() else null
                    val lastModified = if (input.readBoolean()) input.readUTF() else null
                    val count = input.readInt()
                    if (count < 0) return null
                    val segments = ArrayList<SegmentRecord>(count)
                    repeat(count) {
                        val start = input.readLong()
                        val end = input.readLong()
                        val done = input.readBoolean()
                        val crc = input.readLong()
                        segments += SegmentRecord(start, end, done, crc)
                    }
                    PartMeta(url, total, etag, lastModified, segments)
                }
            }.getOrNull()
        }

        private fun syncDir(file: Path) {
            // 元数据落盘后尝试 fsync 所在目录，降低掉电丢失风险（Windows 上可能不支持，忽略失败）
            runCatching {
                java.nio.channels.FileChannel.open(file.parent, java.nio.file.StandardOpenOption.READ).use { it.force(true) }
            }
        }
    }
}
