package com.minecraft.launcher.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtils() {} // 阻止实例化

    public static String toPrettyJson(Object object) {
        if (object == null) {
            return "null";
        }
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            return "Failed to serialize object: " + e.getMessage();
        }
    }

    public static void printPretty(Object object) {
        System.out.println(toPrettyJson(object));
    }
}
