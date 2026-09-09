package com.arthium.finance.mail;

import com.arthium.finance.budget.BudgetStatus;

/** Payload for a single budget threshold alert. Amounts are in dollars. */
public record BudgetAlert(
        String category,
        double limitAmount,
        double spent,
        double remaining,
        double percentageUsed,
        BudgetStatus level,
        String periodLabel
) {
}
