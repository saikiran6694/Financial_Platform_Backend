package com.arthium.finance.report.dto;

import java.util.List;

public record ReportGenerateResponse(
        String message,
        String periodLabel,
        ReportSummary summary,
        List<String> insights
) {
    public static ReportGenerateResponse of(String message, GeneratedReport report) {
        return new ReportGenerateResponse(message, report.periodLabel(), report.summary(), report.insights());
    }
}
