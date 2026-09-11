package com.arthium.finance.report;

import com.arthium.finance.ai.GeminiService;
import com.arthium.finance.common.ApiException;
import com.arthium.finance.common.DateUtils;
import com.arthium.finance.common.MoneyUtils;
import com.arthium.finance.common.PaginationDto;
import com.arthium.finance.cron.DynamicJobScheduler;
import com.arthium.finance.report.dto.GeneratedReport;
import com.arthium.finance.report.dto.ReportListResponse;
import com.arthium.finance.report.dto.ReportResponse;
import com.arthium.finance.report.dto.ReportSettingUpdateRequest;
import com.arthium.finance.report.dto.ReportSummary;
import com.arthium.finance.report.dto.TopCategory;
import com.arthium.finance.transaction.CategoryTotal;
import com.arthium.finance.transaction.SummaryTotals;
import com.arthium.finance.transaction.TransactionRepository;
import com.arthium.finance.transaction.TransactionType;
import com.arthium.finance.user.dto.ReportScheduleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReportService {

    /** Daily budget threshold check at 09:00 in the user's timezone. */
    private static final String BUDGET_CRON = "0 9 * * *";

    private final TransactionRepository transactionRepository;
    private final ReportRepository reportRepository;
    private final ReportSettingsRepository reportSettingsRepository;
    private final ReportScheduleRepository reportScheduleRepository;
    private final GeminiService geminiService;
    private final DynamicJobScheduler jobScheduler;

    public ReportService(TransactionRepository transactionRepository,
                         ReportRepository reportRepository,
                         ReportSettingsRepository reportSettingsRepository,
                         ReportScheduleRepository reportScheduleRepository,
                         GeminiService geminiService,
                         DynamicJobScheduler jobScheduler) {
        this.transactionRepository = transactionRepository;
        this.reportRepository = reportRepository;
        this.reportSettingsRepository = reportSettingsRepository;
        this.reportScheduleRepository = reportScheduleRepository;
        this.geminiService = geminiService;
        this.jobScheduler = jobScheduler;
    }

    // ── Listing ──────────────────────────────────────────────────────────────

    public ReportListResponse getAllReports(int pageNumber, int pageSize, String userId) {
        PageRequest pageRequest = PageRequest.of(
                pageNumber - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Report> page = reportRepository.findByUserId(UUID.fromString(userId), pageRequest);

        return new ReportListResponse(
                "Reports fetched successfully",
                page.getContent().stream().map(ReportResponse::from).toList(),
                PaginationDto.of(pageNumber, pageSize, page.getTotalElements())
        );
    }

    // ── Report generation ────────────────────────────────────────────────────

    /**
     * Port of generate_report_service. Returns null when there was no activity
     * in the period, which the route turns into a 404.
     */
    public GeneratedReport generateReport(String userId, Instant fromDate, Instant toDate) {
        UUID ownerId = UUID.fromString(userId);

        SummaryTotals totals = transactionRepository.sumTotals(ownerId, fromDate, toDate);
        long totalIncome = totals.getTotalIncome();
        long totalExpenses = totals.getTotalExpense();

        if (totalIncome == 0 && totalExpenses == 0) {
            return null;
        }

        List<CategoryTotal> topCategoryTotals = transactionRepository
                .sumByCategory(ownerId, TransactionType.EXPENSE.name(), fromDate, toDate)
                .stream().limit(5).toList();

        // name -> {amount (dollars), percentage}
        Map<String, Map<String, Object>> byCategory = new LinkedHashMap<>();
        for (CategoryTotal category : topCategoryTotals) {
            long total = category.getTotal();
            long percentage = totalExpenses > 0 ? Math.round((total * 100.0) / totalExpenses) : 0;

            Map<String, Object> values = new LinkedHashMap<>();
            values.put("amount", MoneyUtils.toDollars(total));
            values.put("percentage", percentage);
            byCategory.put(category.getCategory(), values);
        }

        long availableBalance = totalIncome - totalExpenses;
        double savingsRate = MoneyUtils.savingRate(totalIncome, totalExpenses);
        String periodLabel = DateUtils.formatPeriodLabel(fromDate, toDate);

        List<String> insights = geminiService.generateAiInsights(
                totalIncome, totalExpenses, availableBalance, savingsRate, byCategory, periodLabel);

        List<TopCategory> topCategories = new ArrayList<>();
        byCategory.forEach((name, values) -> topCategories.add(new TopCategory(
                name,
                ((Number) values.get("amount")).doubleValue(),
                ((Number) values.get("percentage")).doubleValue()
        )));

        ReportSummary summary = new ReportSummary(
                MoneyUtils.toDollars(totalIncome),
                MoneyUtils.toDollars(totalExpenses),
                MoneyUtils.toDollars(availableBalance),
                MoneyUtils.round(savingsRate, 2),
                topCategories
        );

        return new GeneratedReport(periodLabel, summary, insights);
    }

    // ── Settings ─────────────────────────────────────────────────────────────

    public void updateReportSetting(ReportSettingUpdateRequest request, String userId) {
        ReportSettings settings = reportSettingsRepository.findByUserId(UUID.fromString(userId))
                .orElseThrow(() -> ApiException.notFound("Report settings not found"));

        Instant now = Instant.now();
        Instant nextReportDate = settings.getNextReportDate();

        if (request.isEnabled() != null) {
            if (nextReportDate != null && !nextReportDate.isAfter(now)) {
                nextReportDate = DateUtils.nextReportDate(settings.getLastSentDate());
            }
        }

        settings.setEnabled(Boolean.TRUE.equals(request.isEnabled()));
        settings.setNextReportDate(nextReportDate);
        settings.setUpdatedAt(now);

        reportSettingsRepository.save(settings);
    }

    public Optional<ReportSettings> findSettingsByUserId(String userId) {
        return reportSettingsRepository.findByUserId(UUID.fromString(userId));
    }

    // ── Scheduling ───────────────────────────────────────────────────────────

    public void scheduleReportJob(String userId, String timezone, String scheduledTime) {
        Instant now = Instant.now();
        UUID ownerId = UUID.fromString(userId);

        ReportSchedule schedule = reportScheduleRepository.findByUserId(ownerId)
                .orElseGet(() -> {
                    ReportSchedule created = new ReportSchedule();
                    created.setUserId(ownerId);
                    created.setCreatedAt(now);
                    return created;
                });

        schedule.setTimezone(timezone);
        schedule.setScheduledTime(scheduledTime);
        schedule.setUpdatedAt(now);

        reportScheduleRepository.save(schedule);

        jobScheduler.reschedule("report", scheduledTime, timezone, userId);
        jobScheduler.reschedule("transaction", scheduledTime, timezone, userId);
        jobScheduler.reschedule("budget", BUDGET_CRON, timezone, userId);
    }

    public ReportScheduleResponse getReportSchedule(String userId) {
        ReportSchedule schedule = reportScheduleRepository.findByUserId(UUID.fromString(userId))
                .orElseThrow(() -> ApiException.notFound("Report schedule not found"));

        return new ReportScheduleResponse(schedule.getTimezone(), schedule.getScheduledTime());
    }

    public void saveReport(Report report) {
        reportRepository.save(report);
    }

    public void saveSettings(ReportSettings settings) {
        reportSettingsRepository.save(settings);
    }
}
