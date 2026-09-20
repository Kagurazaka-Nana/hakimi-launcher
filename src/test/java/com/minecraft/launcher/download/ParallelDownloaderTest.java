package com.minecraft.launcher.download;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParallelDownloaderTest {

    /** 内存分块抓取器，用于离线测试。 */
    private static final class ByteArrayFetcher implements RangeFetcher {
        final byte[] data;
        int fetchCalls;

        ByteArrayFetcher(byte[] data) {
            this.data = data;
        }

        @Override
        public long size(String url) {
            return data.length;
        }

        @Override
        public byte[] fetch(String url, long start, long end) {
            fetchCalls++;
            if (end < 0) {
                return Arrays.copyOf(data, data.length);
            }
            return Arrays.copyOfRange(data, (int) start, (int) end + 1);
        }
    }

    @Test
    void planChunksCoversWholeFileWithoutOverlap() {
        List<long[]> chunks = ParallelDownloader.planChunks(10, 4);
        assertEquals(3, chunks.size());
        assertArrayEquals(new long[]{0, 3}, chunks.get(0));
        assertArrayEquals(new long[]{4, 7}, chunks.get(1));
        assertArrayEquals(new long[]{8, 9}, chunks.get(2));

        assertEquals(1, ParallelDownloader.planChunks(0, 4).size());
        assertArrayEquals(new long[]{0, -1}, ParallelDownloader.planChunks(-1, 4).get(0));
    }

    @Test
    void downloadAssemblesAndVerifiesDigest(@TempDir Path dir) {
        byte[] content = "hakimi-launcher 并行下载测试内容，重复一些字节以产生多个分块".getBytes(StandardCharsets.UTF_8);
        ByteArrayFetcher fetcher = new ByteArrayFetcher(content);
        Path target = dir.resolve("out.bin");
        String expected = ParallelDownloader.digestHex(writeTemp(dir, content), "SHA-256");

        new ParallelDownloader(fetcher, 4, 8, 3).download("https://example.invalid/file", target, expected, "SHA-256", null);

        assertArrayEquals(content, readAll(target));
        assertTrue(fetcher.fetchCalls >= 1);
    }

    @Test
    void downloadResumesFromExistingParts(@TempDir Path dir) {
        byte[] content = "0123456789".getBytes(StandardCharsets.UTF_8);
        Path partsDir = dir.resolve("out.bin.parts");
        try {
            Files.createDirectories(partsDir);
            Files.write(partsDir.resolve("part-00000"), Arrays.copyOfRange(content, 0, 4));
            Files.write(partsDir.resolve("part-00001"), Arrays.copyOfRange(content, 4, 8));
            Files.write(partsDir.resolve("part-00002"), Arrays.copyOfRange(content, 8, 10));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        RangeFetcher throwing = new RangeFetcher() {
            @Override
            public long size(String url) {
                return content.length;
            }

            @Override
            public byte[] fetch(String url, long start, long end) {
                throw new AssertionError("不应重新下载已完成的分块");
            }
        };

        Path target = dir.resolve("out.bin");
        new ParallelDownloader(throwing, 4, 4, 1).download("https://example.invalid/file", target, null, null, null);
        assertArrayEquals(content, readAll(target));
    }

    @Test
    void digestMatchesKnownSha256Vector() throws IOException {
        Path file = Files.createTempFile("digest", ".txt");
        Files.write(file, "abc".getBytes(StandardCharsets.UTF_8));
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                ParallelDownloader.digestHex(file, "SHA-256"));
        Files.deleteIfExists(file);
    }

    @Test
    void wrongDigestThrows(@TempDir Path dir) {
        ByteArrayFetcher fetcher = new ByteArrayFetcher("hello".getBytes(StandardCharsets.UTF_8));
        Path target = dir.resolve("out.bin");
        assertThrows(DownloadException.class, () ->
                new ParallelDownloader(fetcher, 2, 4, 1).download("https://example.invalid/f", target, "00".repeat(32), "SHA-256", null));
    }

    @Test
    void retriesFlakyChunkThenSucceeds(@TempDir Path dir) {
        byte[] content = "retry-me".getBytes(StandardCharsets.UTF_8);
        final int[] calls = {0};
        RangeFetcher flaky = new RangeFetcher() {
            @Override
            public long size(String url) {
                return content.length;
            }

            @Override
            public byte[] fetch(String url, long start, long end) {
                if (calls[0]++ == 0) {
                    throw new RuntimeException("transient failure");
                }
                return Arrays.copyOfRange(content, (int) start, (int) end + 1);
            }
        };
        Path target = dir.resolve("out.bin");
        new ParallelDownloader(flaky, 2, 4, 3).download("https://example.invalid/f", target, null, null, null);
        assertArrayEquals(content, readAll(target));
        assertTrue(calls[0] >= 2);
    }

    @Test
    void unknownSizeDownloadsWhole(@TempDir Path dir) {
        byte[] content = "unknown-length".getBytes(StandardCharsets.UTF_8);
        RangeFetcher unknown = new RangeFetcher() {
            @Override
            public long size(String url) {
                return -1;
            }

            @Override
            public byte[] fetch(String url, long start, long end) {
                return Arrays.copyOf(content, content.length);
            }
        };
        Path target = dir.resolve("out.bin");
        new ParallelDownloader(unknown, 2, 8, 2).download("https://example.invalid/f", target, null, null, null);
        assertArrayEquals(content, readAll(target));
    }

    @Test
    void retryExhaustedThrows(@TempDir Path dir) {
        RangeFetcher alwaysFails = new RangeFetcher() {
            @Override
            public long size(String url) {
                return 8;
            }

            @Override
            public byte[] fetch(String url, long start, long end) {
                throw new RuntimeException("persistent failure");
            }
        };
        Path target = dir.resolve("out.bin");
        assertThrows(DownloadException.class, () ->
                new ParallelDownloader(alwaysFails, 1, 4, 2).download("https://example.invalid/f", target, null, null, null));
    }

    private static Path writeTemp(Path dir, byte[] content) {
        try {
            Path p = Files.createTempFile(dir, "ref", ".bin");
            Files.write(p, content);
            return p;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] readAll(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
