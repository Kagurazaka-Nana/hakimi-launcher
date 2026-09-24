package com.minecraft.launcher.download.mojang;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 文件摘要校验。Mojang 工件的 sha1 是内容寻址标识（完整性语义，非安全语义）；
 * 算法名收敛为参数，不散布硬编码。
 */
public final class Checksums {

    /** Mojang 版本 JSON 使用的摘要算法。 */
    public static final String MOJANG_DIGEST = "SHA-1";

    private Checksums() {}

    public static String digest(Path file, String algorithm) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("不支持的摘要算法: " + algorithm, e);
        }
        byte[] buffer = new byte[1 << 16];
        try (InputStream in = Files.newInputStream(file)) {
            int read;
            while ((read = in.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    /** 文件摘要是否匹配（expected 为 null 时视为匹配）。 */
    public static boolean matches(Path file, String algorithm, String expectedHex) throws IOException {
        if (expectedHex == null || expectedHex.isBlank()) {
            return true;
        }
        return expectedHex.equalsIgnoreCase(digest(file, algorithm));
    }
}
