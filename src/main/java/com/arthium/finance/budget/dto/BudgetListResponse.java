package com.arthium.finance.budget.dto;

import java.util.List;

public record BudgetListResponse(
        String message,
        double totalLimit,
        double totalSpent,
        List<BudgetItem> budgets
) {
}
