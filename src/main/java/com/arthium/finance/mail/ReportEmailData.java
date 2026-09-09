package com.arthium.finance.mail;

import com.arthium.finance.report.dto.TopCategory;

import java.util.List;

/** Payload for the monthly report email. All amounts are in dollars. */
public record ReportEmailData(
        String period,
        double totalIncome,
        double totalExpenses,
        double availableBalance,
        double savingsRate,
        List<TopCategory> topCategories,
        List<String> insights
) {
}
