package com.arthium.finance.analytics.dto;

import java.util.Map;

public record AnalyticsResponse(String message, Map<String, Object> stats) {
}
