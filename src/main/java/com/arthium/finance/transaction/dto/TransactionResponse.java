package com.arthium.finance.transaction.dto;

import com.arthium.finance.common.MoneyUtils;
import com.arthium.finance.transaction.PaymentMethod;
import com.arthium.finance.transaction.RecurringInterval;
import com.arthium.finance.transaction.Transaction;
import com.arthium.finance.transaction.TransactionStatus;
import com.arthium.finance.transaction.TransactionType;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/** Port of schemas/transactions_schema.py::TransactionResponse. Amount is in DOLLARS. */
public record TransactionResponse(
        @JsonProperty("_id") String id,
        String userId,
        String title,
        TransactionType type,
        double amount,
        String category,
        Instant date,
        boolean isRecurring,
        RecurringInterval recurringInterval,
        String description,
        String receiptUrl,
        PaymentMethod paymentMethod,
        Instant nextRecurringDate,
        Instant lastProcessed,
        TransactionStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static TransactionResponse from(Transaction tx) {
        return new TransactionResponse(
                tx.getId() != null ? tx.getId().toHexString() : null,
                tx.getUserId() != null ? tx.getUserId().toHexString() : null,
                tx.getTitle(),
                tx.getType(),
                MoneyUtils.toDollars(tx.getAmount()),
                tx.getCategory(),
                tx.getDate(),
                tx.isRecurring(),
                tx.getRecurringInterval(),
                tx.getDescription(),
                tx.getReceiptUrl(),
                tx.getPaymentMethod(),
                tx.getNextRecurringDate(),
                tx.getLastProcessed(),
                tx.getStatus(),
                tx.getCreatedAt(),
                tx.getUpdatedAt()
        );
    }
}
