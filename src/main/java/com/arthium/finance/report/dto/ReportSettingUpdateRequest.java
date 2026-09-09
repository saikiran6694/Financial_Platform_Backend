package com.arthium.finance.report.dto;

import jakarta.validation.constraints.NotNull;

/** Port of schemas/report_settings_schema.py::ReportSettingUpdate. */
public record ReportSettingUpdateRequest(@NotNull Boolean isEnabled) {
}
