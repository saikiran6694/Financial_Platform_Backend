package com.arthium.finance.chat.dto;

import jakarta.validation.constraints.NotBlank;

/** Port of schemas/chat_schema.py::ChatHistoryItem. Role is "user" or "assistant". */
public record ChatHistoryItem(@NotBlank String role, @NotBlank String content) {
}
