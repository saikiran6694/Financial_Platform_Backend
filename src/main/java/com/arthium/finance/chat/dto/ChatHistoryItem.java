package com.arthium.finance.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatHistoryItem(@NotBlank String role, @NotBlank String content) {
}
