package com.arthium.finance.report.dto;

import java.util.List;

/**
 * Internal shape returned by ReportService.generateReport, shared by the
 * /api/report/generate endpoint and the monthly report cron job.
 */
public record GeneratedReport(String periodLabel, ReportSummary summary, List<String> insights) {
}
