package com.arthium.finance.user.dto;

import jakarta.validation.constraints.NotBlank;

public record ReportScheduleRequest(
        @NotBlank String timezone,
        @NotBlank String scheduledTime
) {
}
