package com.minecraft.launcher.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MicrosoftServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void extractsUhs() throws Exception {
        var response = mapper.readTree("""
                {"Token":"tok","DisplayClaims":{"xui":[{"uhs":"HASH123","xuid":"123"}]}}
                """);
        assertEquals("HASH123", MicrosoftService.extractUhs(response, null));
    }

    @Test
    void rejectsUhsMismatch() throws Exception {
        var response = mapper.readTree("""
                {"Token":"tok","DisplayClaims":{"xui":[{"uhs":"OTHER"}]}}
                """);
        assertThrows(AuthenticationException.class, () -> MicrosoftService.extractUhs(response, "HASH123"));
    }

    @Test
    void rejectsMissingTokenWithXErr() throws Exception {
        var response = mapper.readTree("""
                {"XErr":2148916233,"Message":"no"}
                """);
        AuthenticationException e = assertThrows(AuthenticationException.class,
                () -> MicrosoftService.extractUhs(response, null));
        assertEquals(true, e.getMessage().contains("2148916233"));
    }

    @Test
    void rejectsMissingUhs() throws Exception {
        var response = mapper.readTree("""
                {"Token":"tok","DisplayClaims":{"xui":[{}]}}
                """);
        assertThrows(AuthenticationException.class, () -> MicrosoftService.extractUhs(response, null));
    }
}
