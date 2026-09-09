package com.arthium.finance.analytics.dto;

import java.util.Map;

/** Port of schemas/analytics_schema.py::AnalyticsResponse. */
public record AnalyticsResponse(String message, Map<String, Object> stats) {
}
