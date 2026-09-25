package com.minecraft.launcher.auth;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkinServiceTest {

    private final SkinService service = new SkinService();

    private static String profileJson(String texturesJson) {
        String b64 = Base64.getEncoder().encodeToString(texturesJson.getBytes(StandardCharsets.UTF_8));
        return """
                {"id":"069a79f444e94726a5befca90e38aaf5","name":"test",
                 "properties":[{"name":"textures","value":"%s"}]}
                """.formatted(b64);
    }

    @Test
    void parsesSkinWithSlimModelAndCape() throws Exception {
        SkinService.Textures textures = service.parseProfile(profileJson("""
                {"textures":{
                  "SKIN":{"url":"http://textures.minecraft.net/texture/abc","metadata":{"model":"slim"}},
                  "CAPE":{"url":"http://textures.minecraft.net/texture/cape"}
                }}
                """));
        assertEquals("http://textures.minecraft.net/texture/abc", textures.skinUrl());
        assertTrue(textures.slim());
        assertEquals("http://textures.minecraft.net/texture/cape", textures.capeUrl());
    }

    @Test
    void parsesClassicSkinWithoutCape() throws Exception {
        SkinService.Textures textures = service.parseProfile(profileJson("""
                {"textures":{"SKIN":{"url":"http://textures.minecraft.net/texture/def"}}}
                """));
        assertEquals("http://textures.minecraft.net/texture/def", textures.skinUrl());
        assertEquals(false, textures.slim());
        assertNull(textures.capeUrl());
    }

    @Test
    void profileWithoutTexturesReturnsNull() throws Exception {
        assertNull(service.parseProfile("{\"id\":\"x\",\"name\":\"y\",\"properties\":[]}"));
    }

    @Test
    void profileWithoutSkinUrlReturnsNull() throws Exception {
        assertNull(service.parseProfile(profileJson("{\"textures\":{}}")));
    }

    @Test
    void defaultSkinResourceIsPng() throws Exception {
        byte[] png = SkinService.defaultSkinBytes();
        assertTrue(png.length > 0 && png.length < 64 * 1024);
        assertEquals((byte) 0x89, png[0]);
        assertEquals((byte) 'P', png[1]);
        assertEquals((byte) 'N', png[2]);
        assertEquals((byte) 'G', png[3]);
    }
}
