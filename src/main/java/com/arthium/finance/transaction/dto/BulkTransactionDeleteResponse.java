package com.arthium.finance.transaction.dto;

public record BulkTransactionDeleteResponse(boolean success, long deletedCount) {
}
