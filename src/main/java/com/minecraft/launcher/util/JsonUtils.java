package com.minecraft.launcher.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;

public class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtils() {} // 阻止实例化

    public static <T> T fromJson(String json, Class<T> type) throws IOException {
        return MAPPER.readValue(json, type);
    }

    public static <T> T fromJson(String json, TypeReference<T> type) throws IOException {
        return MAPPER.readValue(json, type);
    }

    public static <T> T readValue(Path file, Class<T> type) throws IOException {
        return MAPPER.readValue(file.toFile(), type);
    }

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
