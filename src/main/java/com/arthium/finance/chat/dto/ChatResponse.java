package com.arthium.finance.chat.dto;

import java.util.Map;

public record ChatResponse(boolean success, Map<String, Object> data) {
}
