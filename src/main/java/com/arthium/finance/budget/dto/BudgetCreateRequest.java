package com.arthium.finance.budget.dto;

import com.arthium.finance.budget.BudgetPeriod;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record BudgetCreateRequest(
        @NotBlank @Size(min = 1, max = 60) String category,
        @NotNull @Positive Double limitAmount,
        BudgetPeriod period,
        @DecimalMin("0.1") @DecimalMax("1.0") Double alertThreshold
) {
    public BudgetCreateRequest {
        if (period == null) {
            period = BudgetPeriod.MONTHLY;
        }
        if (alertThreshold == null) {
            alertThreshold = 0.8;
        }
    }
}
