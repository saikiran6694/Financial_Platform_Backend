package com.arthium.finance.report.dto;

import com.arthium.finance.report.Report;
import com.arthium.finance.report.ReportStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record ReportResponse(
        @JsonProperty("_id") String id,
        String userId,
        String period,
        Instant sentDate,
        ReportStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getId() != null ? report.getId().toString() : null,
                report.getUserId() != null ? report.getUserId().toString() : null,
                report.getPeriod(),
                report.getSentDate(),
                report.getStatus(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }
}
