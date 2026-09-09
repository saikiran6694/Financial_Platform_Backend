package com.arthium.finance.transaction.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BulkTransactionCreateRequest(
        @NotEmpty @Size(max = 300) @Valid List<TransactionCreateRequest> transactions
) {
    public BulkTransactionCreateRequest {
        if (transactions != null) {
            for (TransactionCreateRequest tx : transactions) {
                if (tx.amount() == null || tx.amount() <= 0 || tx.amount() > 1_000_000_000d) {
                    throw new IllegalArgumentException(
                            "Amount must be a positive number and not exceed 1,000,000,000");
                }
            }
        }
    }
}
