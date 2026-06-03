package org.example.cinemabookingsystem.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

public final class JsonParser {

    private static final ObjectMapper jsonMapper = JsonMapper.builder().build();

    private JsonParser() {
        throw new IllegalStateException("Utility class");
    }

    public static JsonNode parseJson(String input) {
        if (input == null || input.isBlank()) {
            return jsonMapper.createObjectNode();
        }
        try {
            return jsonMapper.readTree(input);
        } catch (JsonProcessingException e) {
            throw new JsonParsingException("Input String has an invalid JSON format.");
        }
    }

    public static String toJsonString(JsonNode node) {
        if (node == null) {
            return null;
        }
        try {
            return jsonMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new JsonParsingException("Failed to serialise JsonNode to String.", e);
        }
    }

    public static JsonNode toJsonArray(Object[] values) {
        ArrayNode arrayNode = jsonMapper.createArrayNode();
        if (values == null) {
            return arrayNode;
        }
        for (Object value : values) {
            arrayNode.add(jsonMapper.valueToTree(value));
        }
        return arrayNode;
    }

    public static Object[] toJavaArray(JsonNode node) {
        if (node == null || node.isNull() || !node.isArray()) {
            return new Object[0];
        }
        Object[] result = new Object[node.size()];
        for (int i = 0; i < node.size(); i++) {
            result[i] = unwrap(node.get(i));
        }
        return result;
    }

    private static Object unwrap(JsonNode element) {
        if (element == null || element.isNull()) {
            return null;
        }
        if (element.isTextual()) {
            return element.asText();
        }
        if (element.isInt()) {
            return element.asInt();
        }
        if (element.isLong()) {
            return element.asLong();
        }
        if (element.isDouble() || element.isFloat()) {
            return element.asDouble();
        }
        if (element.isBoolean()) {
            return element.asBoolean();
        }
        return element.asText();
    }
}
