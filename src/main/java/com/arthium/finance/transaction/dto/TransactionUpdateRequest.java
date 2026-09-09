package com.arthium.finance.transaction.dto;

import com.arthium.finance.transaction.PaymentMethod;
import com.arthium.finance.transaction.RecurringInterval;
import com.arthium.finance.transaction.TransactionStatus;
import com.arthium.finance.transaction.TransactionType;

import java.time.Instant;

public record TransactionUpdateRequest(
        String title,
        TransactionType type,
        Double amount,
        String category,
        Instant date,
        Boolean isRecurring,
        RecurringInterval recurringInterval,
        String description,
        String receiptUrl,
        PaymentMethod paymentMethod,
        TransactionStatus status
) {
}
