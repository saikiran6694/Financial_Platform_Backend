package com.arthium.finance.transaction.dto;

import com.arthium.finance.transaction.PaymentMethod;
import com.arthium.finance.transaction.TransactionType;

import java.time.Instant;

public record ScanReceiptResponse(
        String title,
        double amount,
        Instant date,
        String description,
        String category,
        PaymentMethod paymentMethod,
        TransactionType type,
        String receiptUrl
) {
}
