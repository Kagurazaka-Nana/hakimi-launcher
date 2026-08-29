package com.minecraft.launcher.model.version.arguments.game;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minecraft.launcher.model.version.arguments.Arguments;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameDeserializerTest {

    Arguments args;
    List<Game> g;

    {
        try {
            String testJson = """
                    {"game": [
                        "--arg1",
                        {
                            "rules": [
                              {
                                "action": "allow",
                                "features": {
                                  "is_demo_user": true
                                }
                              }
                            ],
                            "value": "--demo"
                        },
                        {
                            "rules": [
                              {
                                "action": "allow",
                                "features": {
                                  "has_custom_resolution": true
                                }
                              }
                            ],
                            "value": [
                              "--width",
                              "${resolution_width}",
                              "--height",
                              "${resolution_height}"
                            ]
                        }
                    ]}
                    """;
            args = new ObjectMapper().readValue(testJson, Arguments.class);
            g = args.getGame();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testJson() {
        assertNull(g.getFirst().getRules());
        assertEquals("--arg1", g.getFirst().getValue());
        assertFalse(g.get(1).getRules().isEmpty());
        assertInstanceOf(String.class, g.get(1).getValue());
        assertFalse(g.get(2).getRules().isEmpty());
        assertInstanceOf(List.class, g.get(2).getValue());
    }

}