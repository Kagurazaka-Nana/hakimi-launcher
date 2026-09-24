package com.minecraft.launcher.download.mojang;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChecksumsTest {

    @TempDir
    Path dir;

    @Test
    void computesSha1OfKnownVector() throws IOException {
        Path file = dir.resolve("abc.txt");
        Files.writeString(file, "abc", StandardCharsets.US_ASCII);
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", Checksums.digest(file, Checksums.MOJANG_DIGEST));
    }

    @Test
    void nullExpectedAlwaysMatches() throws IOException {
        Path file = dir.resolve("x");
        Files.writeString(file, "anything");
        assertTrue(Checksums.matches(file, Checksums.MOJANG_DIGEST, null));
        assertTrue(Checksums.matches(file, Checksums.MOJANG_DIGEST, "  "));
    }

    @Test
    void mismatchDetectedCaseInsensitively() throws IOException {
        Path file = dir.resolve("abc.txt");
        Files.writeString(file, "abc", StandardCharsets.US_ASCII);
        assertTrue(Checksums.matches(file, Checksums.MOJANG_DIGEST, "A9993E364706816ABA3E25717850C26C9CD0D89D"));
        assertFalse(Checksums.matches(file, Checksums.MOJANG_DIGEST, "0000000000000000000000000000000000000000"));
    }

    @Test
    void unknownAlgorithmThrows() throws IOException {
        Path file = dir.resolve("x");
        Files.writeString(file, "x");
        assertThrows(IOException.class, () -> Checksums.digest(file, "NOT-AN-ALGORITHM"));
    }
}
