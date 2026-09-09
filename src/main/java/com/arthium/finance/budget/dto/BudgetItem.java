package com.arthium.finance.budget.dto;

import com.arthium.finance.budget.BudgetPeriod;
import com.arthium.finance.budget.BudgetStatus;

public record BudgetItem(
        String id,
        String category,
        double limitAmount,
        double spent,
        double remaining,
        double percentageUsed,
        BudgetStatus status,
        double alertThreshold,
        boolean isActive,
        BudgetPeriod period
) {
}
