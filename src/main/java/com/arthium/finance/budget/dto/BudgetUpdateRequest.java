package com.arthium.finance.budget.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;


public record BudgetUpdateRequest(
        @Positive Double limitAmount,
        @DecimalMin("0.1") @DecimalMax("1.0") Double alertThreshold,
        Boolean isActive
) {
}
