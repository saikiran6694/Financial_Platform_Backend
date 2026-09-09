package com.arthium.finance.report;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Port of models/report_models.py::DateRangeEnum.
 * The wire values differ from the constant names, so they are carried
 * explicitly and used for both query-parameter binding and JSON output.
 */
public enum DateRange {

    LAST_30_DAYS("30days"),
    LAST_MONTH("lastMonth"),
    LAST_3_MONTHS("last3Months"),
    LAST_YEAR("lastYear"),
    THIS_MONTH("thisMonth"),
    THIS_YEAR("thisYear"),
    ALL_TIME("allTime"),
    CUSTOM("custom");

    private final String value;

    DateRange(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static DateRange fromValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        for (DateRange range : values()) {
            if (range.value.equalsIgnoreCase(raw.trim()) || range.name().equalsIgnoreCase(raw.trim())) {
                return range;
            }
        }
        throw new IllegalArgumentException("Unknown date range preset: " + raw);
    }
}
