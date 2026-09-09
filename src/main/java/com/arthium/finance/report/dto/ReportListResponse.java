package com.arthium.finance.report.dto;

import com.arthium.finance.common.PaginationDto;

import java.util.List;

public record ReportListResponse(String message, List<ReportResponse> reports, PaginationDto pagination) {
}
