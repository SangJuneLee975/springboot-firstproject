package com.example.apitest.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;

public class JsonUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String listToJson(List<String> list) throws JsonProcessingException {
        return objectMapper.writeValueAsString(list);
    }

    public static List<String> jsonToList(String json) throws IOException {
        return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
    }
}
