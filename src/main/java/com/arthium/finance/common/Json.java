package com.arthium.finance.common;

import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/**
 * Thin wrapper around the auto-configured Jackson mapper.
 *
 * Spring Boot 4 ships Jackson 3 (package tools.jackson.*). Everything else in
 * the codebase works with plain Map/List structures and never imports Jackson
 * directly, so swapping the JSON library only means editing this one file.
 */
@Component
public class Json {

    private final ObjectMapper mapper;

    public Json(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String write(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to serialise value to JSON", e);
        }
    }

    /** Returns null when the payload is not a JSON object. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> readMap(String raw) {
        try {
            Object parsed = mapper.readValue(raw, Object.class);
            return parsed instanceof Map ? (Map<String, Object>) parsed : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Returns null when the payload is not a JSON array. */
    @SuppressWarnings("unchecked")
    public List<Object> readList(String raw) {
        try {
            Object parsed = mapper.readValue(raw, Object.class);
            return parsed instanceof List ? (List<Object>) parsed : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Strips the ```json fences models like to wrap their output in. */
    public static String stripCodeFences(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("```json", "").replace("```", "").trim();
    }

    // ── Null-safe navigation over parsed Map/List structures ────────────────

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> asList(Object value) {
        return value instanceof List ? (List<Object>) value : null;
    }

    public static Object get(Map<String, Object> source, String key) {
        return source == null ? null : source.get(key);
    }

    public static String str(Map<String, Object> source, String key) {
        Object value = get(source, key);
        return value == null ? null : String.valueOf(value);
    }

    public static Double dbl(Map<String, Object> source, String key) {
        Object value = get(source, key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
