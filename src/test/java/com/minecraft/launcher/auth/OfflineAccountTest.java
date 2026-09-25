package com.minecraft.launcher.auth;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfflineAccountTest {

    @Test
    void nameToUuidIsDeterministicAndVersion3() {
        UUID first = OfflineAccount.nameToUuid("hakimi");
        UUID second = OfflineAccount.nameToUuid("hakimi");
        assertEquals(first, second, "同名必须得到同一 UUID（原版离线协议要求）");
        assertEquals(3, first.version(), "应为 MD5 name-based（version 3）UUID");
        assertNotEquals(first, OfflineAccount.nameToUuid("Hakimi2"));
    }

    @Test
    void logInReturnsFakeTokenWithoutNetwork() throws Exception {
        OfflineAccount account = new OfflineAccount("tester");
        AuthInfo info = account.logIn();
        assertEquals("tester", info.getUsername());
        assertEquals(OfflineAccount.nameToUuid("tester"), info.getUuid());
        assertEquals("msa", info.getUserType());
        assertEquals(32, info.getAccessToken().length(), "随机 token 应为无横线 UUID");
        assertNotNull(account.toString());
    }

    @Test
    void authInfoToStringMasksToken() throws Exception {
        AuthInfo info = new OfflineAccount("t").logIn();
        assertTrue(!info.toString().contains(info.getAccessToken()), "toString 必须脱敏 accessToken");
    }

    @Test
    void customUuidOverridesDefault() {
        UUID custom = UUID.randomUUID();
        OfflineAccount account = new OfflineAccount("x", custom);
        assertEquals(custom, account.getProfileID());
        assertEquals("offline", account.type());
    }
}
