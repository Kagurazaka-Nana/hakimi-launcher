package com.minecraft.launcher.download;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Function;

/**
 * 下载目标的 SSRF 防护：仅允许 http/https，且 host 解析出的所有地址
 * 都不得是环回 / 本机任意地址 / 链路本地 / 站点本地 / 组播 / 保留网段。
 */
public final class UrlGuard {

    private UrlGuard() {}

    public static URI validate(String url) {
        return validate(url, hostname -> {
            try {
                return InetAddress.getAllByName(hostname);
            } catch (Exception e) {
                throw new SecurityException("主机名解析失败: " + hostname, e);
            }
        });
    }

    /** 校验并返回规范化后的 URI；不合法时抛 SecurityException。resolve 可注入（测试用）。 */
    public static URI validate(String url, Function<String, InetAddress[]> resolve) {
        URI uri;
        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException e) {
            throw new SecurityException("无法解析的下载地址: " + url, e);
        }
        String scheme = uri.getScheme() == null ? null : uri.getScheme().toLowerCase();
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new SecurityException("仅允许 http/https 协议: " + scheme);
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new SecurityException("下载地址缺少主机名");
        }
        InetAddress[] addresses;
        try {
            addresses = resolve.apply(host);
        } catch (RuntimeException e) {
            throw new SecurityException("主机名解析失败: " + host, e);
        }
        if (addresses == null || addresses.length == 0) {
            throw new SecurityException("主机名解析结果为空: " + host);
        }
        for (InetAddress addr : addresses) {
            if (isForbidden(addr)) {
                throw new SecurityException("拒绝访问保留/内网地址: " + host + " -> " + addr.getHostAddress());
            }
        }
        return uri;
    }

    public static boolean isForbidden(InetAddress addr) {
        if (addr instanceof Inet4Address v4) {
            return isForbiddenV4(v4);
        }
        if (addr instanceof Inet6Address v6) {
            return isForbiddenV6(v6);
        }
        return true;
    }

    private static boolean isForbiddenV4(Inet4Address addr) {
        if (addr.isLoopbackAddress() || addr.isAnyLocalAddress() || addr.isLinkLocalAddress()
                || addr.isMulticastAddress() || addr.isSiteLocalAddress()) {
            return true;
        }
        byte[] b = addr.getAddress();
        int first = b[0] & 0xFF;
        int second = b[1] & 0xFF;
        int third = b[2] & 0xFF;
        if (first == 0) return true;                       // 0.0.0.0/8 本网络
        if (first == 100 && second >= 64 && second <= 127) return true; // 100.64.0.0/10 运营商级 NAT
        if (first == 192 && second == 0 && third == 0) return true;     // 192.0.0.0/24
        if (first == 192 && second == 0 && third == 2) return true;     // TEST-NET-1
        if (first == 198 && (second == 18 || second == 19)) return true; // 198.18.0.0/15 基准测试
        if (first == 198 && second == 51 && third == 100) return true;  // TEST-NET-2
        if (first == 203 && second == 0 && third == 113) return true;   // TEST-NET-3
        return first >= 240;                               // 240.0.0.0/4 保留 + 广播
    }

    private static boolean isForbiddenV6(Inet6Address addr) {
        if (addr.isLoopbackAddress() || addr.isAnyLocalAddress() || addr.isLinkLocalAddress()
                || addr.isMulticastAddress() || addr.isSiteLocalAddress()) {
            return true;
        }
        byte[] b = addr.getAddress();
        if ((b[0] & 0xFE) == 0xFC) {
            return true; // fc00::/7 唯一本地地址（ULA）
        }
        // IPv4 映射地址 ::ffff:x.y.z.w 交给 IPv4 规则复核
        if (b[0] == 0 && b[1] == 0 && b[10] == (byte) 0xFF && b[11] == (byte) 0xFF) {
            byte[] v4 = new byte[]{b[12], b[13], b[14], b[15]};
            try {
                InetAddress mapped = InetAddress.getByAddress(v4);
                if (mapped instanceof Inet4Address a && isForbiddenV4(a)) {
                    return true;
                }
            } catch (Exception ignored) {
                return true;
            }
        }
        return false;
    }
}
