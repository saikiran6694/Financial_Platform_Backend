package com.arthium.finance.chat.dto;

import java.util.Map;

/**
 * Port of schemas/chat_schema.py::ChatResponse.
 * `data` is whichever structured shape the model chose (text, bullets, table,
 * chart, advice or whatif), passed through as-is.
 */
public record ChatResponse(boolean success, Map<String, Object> data) {
}
