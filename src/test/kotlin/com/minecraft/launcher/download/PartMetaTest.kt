package com.minecraft.launcher.download

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class PartMetaTest {

    @TempDir
    lateinit var dir: Path

    private fun sample() = PartMeta(
        "https://example.com/a.zip",
        1024,
        "\"abc\"",
        "Mon, 22 Sep 2026 00:00:00 GMT",
        listOf(
            PartMeta.SegmentRecord(0, 511, true, 0x1234),
            PartMeta.SegmentRecord(512, 1023, false, 0),
        ),
    )

    @Test
    fun `write then read roundtrips`() {
        val file = dir.resolve("a.zip.part.meta")
        sample().write(file)
        val loaded = PartMeta.read(file)!!
        assertEquals(sample(), loaded)
        assertEquals(listOf(Segment(512, 1023)), loaded.pendingSegments())
        assertEquals(1, loaded.segments().count { it.done() })
    }

    @Test
    fun `missing file reads as null`() {
        assertNull(PartMeta.read(dir.resolve("nope.meta")))
    }

    @Test
    fun `corrupt content reads as null`() {
        val file = dir.resolve("bad.meta")
        Files.write(file, byteArrayOf(1, 2, 3))
        assertNull(PartMeta.read(file))
        // 魔数不对
        val file2 = dir.resolve("bad2.meta")
        java.io.DataOutputStream(Files.newOutputStream(file2)).use { it.writeInt(0x12345678) }
        assertNull(PartMeta.read(file2))
    }

    @Test
    fun `matches is strict on url total etag`() {
        val meta = sample()
        assertTrue(meta.matches("https://example.com/a.zip", 1024, "\"abc\"", "Mon, 22 Sep 2026 00:00:00 GMT"))
        assertFalse(meta.matches("https://example.com/b.zip", 1024, "\"abc\"", null))
        assertFalse(meta.matches("https://example.com/a.zip", 2048, "\"abc\"", "Mon, 22 Sep 2026 00:00:00 GMT"))
        assertFalse(meta.matches("https://example.com/a.zip", 1024, "\"def\"", "Mon, 22 Sep 2026 00:00:00 GMT"))
    }

    @Test
    fun `atomic write leaves no tmp file`() {
        val file = dir.resolve("x.zip.part.meta")
        sample().write(file)
        assertTrue(Files.exists(file))
        assertFalse(Files.exists(file.resolveSibling("x.zip.part.meta.tmp")))
    }
}
