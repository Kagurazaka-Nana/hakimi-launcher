package com.minecraft.launcher.download;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrlValidatorTest {

    @Test
    void rejectsNonHttpSchemes() {
        assertThrows(IllegalArgumentException.class, () -> UrlValidator.validate("file:///etc/passwd"));
        assertThrows(IllegalArgumentException.class, () -> UrlValidator.validate("ftp://example.com/x"));
        assertThrows(IllegalArgumentException.class, () -> UrlValidator.validate("gopher://1.1.1.1"));
        assertThrows(IllegalArgumentException.class, () -> UrlValidator.validate(""));
    }

    @Test
    void rejectsLoopbackAndPrivateAddresses() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> UrlValidator.validate("http://127.0.0.1/x"));
        assertThrows(IllegalArgumentException.class, () -> UrlValidator.validate("https://10.0.0.5/x"));
        assertThrows(IllegalArgumentException.class, () -> UrlValidator.validate("http://192.168.1.1/x"));
        assertThrows(IllegalArgumentException.class, () -> UrlValidator.validate("http://169.254.169.254/latest/meta-data"));
        assertThrows(IllegalArgumentException.class, () -> UrlValidator.validate("http://[::1]/x"));
        assertThrows(IllegalArgumentException.class, () -> UrlValidator.validate("http://0.0.0.0/x"));
    }

    @Test
    void acceptsPublicAddresses() {
        assertDoesNotThrow(() -> UrlValidator.validate("https://8.8.8.8/dns-query"));
        assertDoesNotThrow(() -> UrlValidator.validate("https://1.1.1.1/"));
        assertDoesNotThrow(() -> UrlValidator.validate("http://[2606:4700:4700::1111]/x"));
    }

    @Test
    void isForbiddenCoversReservedRanges() throws UnknownHostException {
        assertTrue(UrlValidator.isForbidden(InetAddress.getByName("127.0.0.1")));
        assertTrue(UrlValidator.isForbidden(InetAddress.getByName("10.1.2.3")));
        assertTrue(UrlValidator.isForbidden(InetAddress.getByName("172.16.0.1")));
        assertTrue(UrlValidator.isForbidden(InetAddress.getByName("100.64.0.1")));
        assertTrue(UrlValidator.isForbidden(InetAddress.getByName("198.18.0.1")));
        assertTrue(UrlValidator.isForbidden(InetAddress.getByName("224.0.0.1")));
        assertTrue(UrlValidator.isForbidden(InetAddress.getByName("::1")));
        assertTrue(UrlValidator.isForbidden(InetAddress.getByName("fe80::1")));
    }
}
