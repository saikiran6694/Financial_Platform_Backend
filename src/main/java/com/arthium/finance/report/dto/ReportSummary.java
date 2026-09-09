package com.arthium.finance.report.dto;

import java.util.List;

public record ReportSummary(
        double income,
        double expenses,
        double balance,
        double savingRate,
        List<TopCategory> topCategories
) {
}
