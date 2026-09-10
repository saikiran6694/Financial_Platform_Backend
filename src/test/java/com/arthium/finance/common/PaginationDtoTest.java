package com.arthium.finance.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaginationDtoTest {

    @Test
    void of_firstPage_computesSkipZeroAndCeilingTotalPages() {
        PaginationDto result = PaginationDto.of(1, 20, 45);

        assertThat(result.skip()).isEqualTo(0);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.totalCount()).isEqualTo(45);
        assertThat(result.pageNumber()).isEqualTo(1);
        assertThat(result.pageSize()).isEqualTo(20);
    }

    @Test
    void of_secondPage_computesNonZeroSkip() {
        PaginationDto result = PaginationDto.of(2, 20, 45);

        assertThat(result.skip()).isEqualTo(20);
        assertThat(result.totalPages()).isEqualTo(3);
    }

    @Test
    void of_totalCountEvenlyDivisibleByPageSize_doesNotAddExtraPage() {
        PaginationDto result = PaginationDto.of(1, 20, 40);

        assertThat(result.totalPages()).isEqualTo(2);
    }

    @Test
    void of_pageSizeZero_avoidsDivideByZeroAndReturnsZeroTotalPages() {
        PaginationDto result = PaginationDto.of(1, 0, 45);

        assertThat(result.totalPages()).isEqualTo(0);
        assertThat(result.skip()).isEqualTo(0);
    }

    @Test
    void of_emptyResultSet_returnsZeroTotalPages() {
        PaginationDto result = PaginationDto.of(1, 20, 0);

        assertThat(result.totalPages()).isEqualTo(0);
    }
}
