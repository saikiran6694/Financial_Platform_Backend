package com.arthium.finance.report.dto;

import java.util.List;

/** Port of schemas/report_schema.py::ReportSummary. */
public record ReportSummary(
        double income,
        double expenses,
        double balance,
        double savingRate,
        List<TopCategory> topCategories
) {
}
