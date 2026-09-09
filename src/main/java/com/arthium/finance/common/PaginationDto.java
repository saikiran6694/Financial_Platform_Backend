package com.arthium.finance.common;

public record PaginationDto(
        int pageSize,
        int pageNumber,
        long totalCount,
        long totalPages,
        long skip
) {
    public static PaginationDto of(int pageNumber, int pageSize, long totalCount) {
        long skip = (long) (pageNumber - 1) * pageSize;
        long totalPages = pageSize == 0 ? 0 : (totalCount + pageSize - 1) / pageSize;
        return new PaginationDto(pageSize, pageNumber, totalCount, totalPages, skip);
    }
}
