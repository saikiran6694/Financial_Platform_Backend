package com.arthium.finance.chat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Port of schemas/chat_schema.py::ChatRequest. */
public record ChatRequest(
        @NotBlank @Size(min = 1, max = 2000) String message,
        @Size(max = 50) @Valid List<ChatHistoryItem> history
) {
    public ChatRequest {
        if (history == null) {
            history = List.of();
        }
    }
}
