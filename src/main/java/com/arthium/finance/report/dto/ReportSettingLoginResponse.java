package com.arthium.finance.report.dto;

import com.arthium.finance.report.ReportFrequency;
import com.arthium.finance.report.ReportSettings;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ReportSettingLoginResponse(
        @JsonProperty("_id") String id,
        ReportFrequency frequency,
        boolean isEnabled
) {
    public static ReportSettingLoginResponse from(ReportSettings settings) {
        if (settings == null) {
            return null;
        }
        return new ReportSettingLoginResponse(
                settings.getId() != null ? settings.getId().toString() : null,
                settings.getFrequency(),
                settings.isEnabled()
        );
    }
}
