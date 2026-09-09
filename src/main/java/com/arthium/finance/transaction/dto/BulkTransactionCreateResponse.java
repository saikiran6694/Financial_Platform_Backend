package com.arthium.finance.transaction.dto;

public record BulkTransactionCreateResponse(int insertedCount, boolean success) {
}
