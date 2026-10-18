package com.oneroute.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.List;
import java.util.Map;

/** Internal native-protocol codec. Spring handles HTTP JSON independently. */
public final class Json {
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();
    static {
        MAPPER.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder().maxNestingDepth(40).build());
    }
    private Json() { }

    public static Object parse(String text) {
        try {
            return MAPPER.readValue(text, Object.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid JSON", exception);
        }
    }

    public static String stringify(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Cannot encode JSON", exception);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("Expected JSON object");
        }
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> array(Object value) {
        if (!(value instanceof List<?>)) {
            throw new IllegalArgumentException("Expected JSON array");
        }
        return (List<Object>) value;
    }
}
