package com.arthium.finance.report.dto;

import jakarta.validation.constraints.NotNull;

public record ReportSettingUpdateRequest(@NotNull Boolean isEnabled) {
}
