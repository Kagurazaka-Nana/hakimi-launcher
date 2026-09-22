package com.minecraft.launcher.download

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.InetAddress

class UrlGuardTest {

    private fun resolveTo(vararg addresses: String): (String) -> Array<InetAddress> =
        { _ -> addresses.map { InetAddress.getByAddress(parseV4(it)) }.toTypedArray() }

    private fun parseV4(v4: String) = v4.split(".").map { it.toInt().toByte() }.toByteArray()

    @Test
    fun `accepts public http and https urls`() {
        val uri = UrlGuard.validate("https://example.com/file.zip", resolveTo("93.184.216.34"))
        assertTrue(uri.scheme == "https")
        assertDoesNotThrow { UrlGuard.validate("http://cdn.example.net:8080/a.bin", resolveTo("1.2.3.4")) }
    }

    @Test
    fun `rejects non http schemes`() {
        assertThrows(SecurityException::class.java) { UrlGuard.validate("ftp://example.com/a", resolveTo("1.2.3.4")) }
        assertThrows(SecurityException::class.java) { UrlGuard.validate("file:///etc/passwd", resolveTo("1.2.3.4")) }
        assertThrows(SecurityException::class.java) { UrlGuard.validate("javascript:alert(1)", resolveTo("1.2.3.4")) }
    }

    @Test
    fun `rejects loopback and any-local`() {
        assertThrows(SecurityException::class.java) { UrlGuard.validate("http://a.com/f", resolveTo("127.0.0.1")) }
        assertThrows(SecurityException::class.java) { UrlGuard.validate("http://a.com/f", resolveTo("0.0.0.0")) }
        // IPv6 环回：用 Inet6Address 构造
        val v6loop = InetAddress.getByName("::1")
        assertTrue(UrlGuard.isForbidden(v6loop))
    }

    @Test
    fun `rejects private and site-local ranges`() {
        listOf("10.0.0.5", "172.16.0.1", "172.31.255.254", "192.168.1.1", "169.254.1.1").forEach { ip ->
            assertThrows(SecurityException::class.java) {
                UrlGuard.validate("http://a.com/f", resolveTo(ip))
            }
        }
    }

    @Test
    fun `rejects cgn benchmark and reserved ranges`() {
        listOf("100.64.0.1", "198.18.0.2", "198.19.255.255", "192.0.2.9", "198.51.100.7", "203.0.113.9", "240.0.0.1", "255.255.255.255").forEach { ip ->
            assertThrows(SecurityException::class.java) {
                UrlGuard.validate("http://a.com/f", resolveTo(ip))
            }
        }
    }

    @Test
    fun `rejects ipv6 unique local and mapped v4`() {
        assertTrue(UrlGuard.isForbidden(InetAddress.getByName("fc00::1234")))
        assertTrue(UrlGuard.isForbidden(InetAddress.getByName("fe80::1")))
        assertTrue(UrlGuard.isForbidden(InetAddress.getByName("::ffff:127.0.0.1")))
        assertTrue(UrlGuard.isForbidden(InetAddress.getByName("::ffff:10.0.0.1")))
        assertFalse(UrlGuard.isForbidden(InetAddress.getByName("2606:2800:220:1:248:1893:25c8:1946")))
    }

    @Test
    fun `rejects missing host and unresolvable`() {
        assertThrows(SecurityException::class.java) { UrlGuard.validate("http:///path", resolveTo("1.2.3.4")) }
        assertThrows(SecurityException::class.java) {
            UrlGuard.validate("http://nonexistent.invalid/f") { error("no dns") }
        }
    }

    @Test
    fun `rejects when any resolved address is forbidden`() {
        // 双解析结果中只要有一个命中内网即拒绝（防 DNS rebinding 局部绕过）
        assertThrows(SecurityException::class.java) {
            UrlGuard.validate("http://a.com/f", resolveTo("1.2.3.4", "10.0.0.1"))
        }
    }
}
