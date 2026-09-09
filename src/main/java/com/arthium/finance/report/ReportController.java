package com.arthium.finance.report;

import com.arthium.finance.common.ApiException;
import com.arthium.finance.report.dto.GeneratedReport;
import com.arthium.finance.report.dto.ReportGenerateResponse;
import com.arthium.finance.report.dto.ReportListResponse;
import com.arthium.finance.report.dto.ReportSettingUpdateRequest;
import com.arthium.finance.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/** Port of routes/reports.py. */
@RestController
@RequestMapping("/api/report")
@Validated
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/all")
    public ReportListResponse getAllReports(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(name = "page_size", defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @RequestParam(name = "page_number", defaultValue = "1") @Min(1) int pageNumber) {

        return reportService.getAllReports(pageNumber, pageSize, currentUser.getIdAsString());
    }

    @GetMapping("/generate")
    public ReportGenerateResponse generateReport(
            @AuthenticationPrincipal User currentUser,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate) {

        GeneratedReport report = reportService.generateReport(currentUser.getIdAsString(), fromDate, toDate);

        if (report == null) {
            throw ApiException.notFound("No activity found for the selected period");
        }

        return ReportGenerateResponse.of("Report Generated Successfully", report);
    }

    @PutMapping("/update-setting")
    public Map<String, String> updateReportSetting(@AuthenticationPrincipal User currentUser,
                                                   @Valid @RequestBody ReportSettingUpdateRequest request) {
        reportService.updateReportSetting(request, currentUser.getIdAsString());
        return Map.of("message", "Report Settings update successfully");
    }
}
