package com.arthium.finance.transaction.dto;

import com.arthium.finance.transaction.PaymentMethod;
import com.arthium.finance.transaction.RecurringInterval;
import com.arthium.finance.transaction.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** Port of schemas/transactions_schema.py::TransactionCreate. Amount is in DOLLARS. */
public record TransactionCreateRequest(
        @NotBlank String title,
        @NotNull TransactionType type,
        @NotNull @Positive Double amount,
        @NotBlank String category,
        Instant date,
        Boolean isRecurring,
        RecurringInterval recurringInterval,
        @Size(max = 250) String description,
        String receiptUrl,
        PaymentMethod paymentMethod
) {
    public TransactionCreateRequest {
        if (date == null) {
            date = Instant.now();
        }
        if (isRecurring == null) {
            isRecurring = Boolean.FALSE;
        }
        if (paymentMethod == null) {
            paymentMethod = PaymentMethod.CASH;
        }
    }
}
