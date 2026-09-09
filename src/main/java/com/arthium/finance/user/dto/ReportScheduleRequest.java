package com.arthium.finance.user.dto;

import jakarta.validation.constraints.NotBlank;

/** Port of schemas/user_schema.py::ReportScheduleData. */
public record ReportScheduleRequest(
        @NotBlank String timezone,
        @NotBlank String scheduledTime
) {
}
