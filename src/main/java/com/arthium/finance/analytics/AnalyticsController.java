package com.arthium.finance.analytics;

import com.arthium.finance.analytics.dto.AnalyticsResponse;
import com.arthium.finance.report.DateRange;
import com.arthium.finance.user.User;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public AnalyticsResponse summary(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) DateRange preset,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate) {

        return new AnalyticsResponse("Summary fetched successfully",
                analyticsService.summary(currentUser.getIdAsString(), preset, fromDate, toDate));
    }

    @GetMapping("/chart")
    public AnalyticsResponse chart(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) DateRange preset,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate) {

        return new AnalyticsResponse("Chart fetched successfully",
                analyticsService.chart(currentUser.getIdAsString(), preset, fromDate, toDate));
    }

    @GetMapping("/expense-breakdown")
    public AnalyticsResponse expenseBreakdown(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) DateRange preset,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate) {

        return new AnalyticsResponse("Expense Breakdown fetched successfully",
                analyticsService.expenseBreakdown(currentUser.getIdAsString(), preset, fromDate, toDate));
    }
}
