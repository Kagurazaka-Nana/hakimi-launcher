package com.minecraft.launcher.download;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

/**
 * 服务端请求 URL 的安全校验（防 SSRF）。
 *
 * 规则：仅允许 http/https；host 必须可解析，且解析到的任一地址不得是
 * 环回、任意本地、链路本地、站点本地(私有)、组播或保留地址。
 */
public final class UrlValidator {

    private UrlValidator() {}

    /** 校验并返回规范化 URI；不合法时抛 {@link IllegalArgumentException}。 */
    public static URI validate(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL 不能为空");
        }
        URI uri;
        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("URL 语法非法: " + url, e);
        }

        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("仅允许 http/https 协议: " + url);
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL 缺少 host: " + url);
        }

        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("无法解析 host: " + host, e);
        }
        for (InetAddress address : addresses) {
            if (isForbidden(address)) {
                throw new IllegalArgumentException("拒绝访问非公网地址: " + host + " -> " + address.getHostAddress());
            }
        }
        return uri;
    }

    /** 判断地址是否属于被禁止的环回/私有/链路本地/保留/组播范围。 */
    public static boolean isForbidden(InetAddress address) {
        if (address.isLoopbackAddress()
                || address.isAnyLocalAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }

        byte[] bytes = address.getAddress();
        if (bytes == null) {
            return true;
        }

        // IPv4 额外保留段（InetAddress 未覆盖的部分）
        if (bytes.length == 4) {
            int b0 = bytes[0] & 0xFF;
            int b1 = bytes[1] & 0xFF;
            // 100.64.0.0/10 CGNAT
            if (b0 == 100 && (b1 & 0xC0) == 64) return true;
            // 198.18.0.0/15 基准测试
            if (b0 == 198 && (b1 == 18 || b1 == 19)) return true;
            // 192.0.0.0/24 IETF 保留
            if (b0 == 192 && b1 == 0 && (bytes[2] & 0xFF) == 0) return true;
            // 192.0.2.0/24、198.51.100.0/24、203.0.113.0/24 文档/示例段
            if (b0 == 192 && b1 == 0 && (bytes[2] & 0xFF) == 2) return true;
            if (b0 == 198 && b1 == 51 && (bytes[2] & 0xFF) == 100) return true;
            if (b0 == 203 && b1 == 0 && (bytes[2] & 0xFF) == 113) return true;
            // 240.0.0.0/4 保留
            if (b0 >= 240) return true;
        }
        return false;
    }
}
