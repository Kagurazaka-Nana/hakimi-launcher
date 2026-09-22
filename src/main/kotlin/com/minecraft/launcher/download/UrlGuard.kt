package com.minecraft.launcher.download

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI

/**
 * 下载目标的 SSRF 防护：仅允许 http/https，且 host 解析出的所有地址
 * 都不得是环回 / 本机任意地址 / 链路本地 / 站点本地 / 组播 / 保留网段。
 */
object UrlGuard {

    /** 校验并返回规范化后的 [URI]，不合法时抛 [SecurityException]。 */
    fun validate(url: String, resolve: (String) -> Array<InetAddress> = InetAddress::getAllByName): URI {
        val uri = runCatching { URI(url.trim()) }.getOrElse {
            throw SecurityException("无法解析的下载地址: $url", it)
        }
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            throw SecurityException("仅允许 http/https 协议: $scheme")
        }
        val host = uri.host
        if (host.isNullOrBlank()) {
            throw SecurityException("下载地址缺少主机名")
        }
        val addresses = runCatching { resolve(host) }.getOrElse {
            throw SecurityException("主机名解析失败: $host", it)
        }
        if (addresses.isEmpty()) {
            throw SecurityException("主机名解析结果为空: $host")
        }
        addresses.firstOrNull { isForbidden(it) }?.let {
            throw SecurityException("拒绝访问保留/内网地址: $host -> ${it.hostAddress}")
        }
        return uri
    }

    fun isForbidden(addr: InetAddress): Boolean = when (addr) {
        is Inet4Address -> isForbiddenV4(addr)
        is Inet6Address -> isForbiddenV6(addr)
        else -> true
    }

    private fun isForbiddenV4(addr: Inet4Address): Boolean {
        if (addr.isLoopbackAddress || addr.isAnyLocalAddress || addr.isLinkLocalAddress ||
            addr.isMulticastAddress || addr.isSiteLocalAddress
        ) {
            return true
        }
        val b = addr.address
        val first = b[0].toInt() and 0xFF
        val second = b[1].toInt() and 0xFF
        return when {
            first == 0 -> true // 0.0.0.0/8 "本网络"
            first == 100 && second in 64..127 -> true // 100.64.0.0/10 运营商级 NAT
            first == 192 && second == 0 && (b[2].toInt() and 0xFF) == 0 -> true // 192.0.0.0/24
            first == 192 && second == 0 && (b[2].toInt() and 0xFF) == 2 -> true // TEST-NET-1
            first == 198 && second in 18..19 -> true // 198.18.0.0/15 基准测试
            first == 198 && second == 51 && (b[2].toInt() and 0xFF) == 100 -> true // TEST-NET-2
            first == 203 && second == 0 && (b[2].toInt() and 0xFF) == 113 -> true // TEST-NET-3
            first >= 240 -> true // 240.0.0.0/4 保留 + 255.255.255.255 广播
            else -> false
        }
    }

    private fun isForbiddenV6(addr: Inet6Address): Boolean {
        if (addr.isLoopbackAddress || addr.isAnyLocalAddress || addr.isLinkLocalAddress ||
            addr.isMulticastAddress || addr.isSiteLocalAddress
        ) {
            return true
        }
        val b = addr.address
        // fc00::/7 唯一本地地址（ULA）
        if ((b[0].toInt() and 0xFE) == 0xFC) return true
        // IPv4 映射地址 ::ffff:x.y.z.w 交给 IPv4 规则复核
        if (b[0] == 0.toByte() && b[1] == 0.toByte() && b[10] == 0xFF.toByte() && b[11] == 0xFF.toByte()) {
            val v4 = InetAddress.getByAddress(b.copyOfRange(12, 16))
            if (v4 is Inet4Address && isForbiddenV4(v4)) return true
        }
        return false
    }
}
