package com.arthium.finance.transaction.dto;

import com.arthium.finance.common.PaginationDto;

import java.util.List;

public record TransactionListResponse(
        String message,
        List<TransactionResponse> transactions,
        PaginationDto pagination
) {
}
