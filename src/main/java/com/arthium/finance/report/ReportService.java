package com.arthium.finance.report;

import com.arthium.finance.ai.GeminiService;
import com.arthium.finance.common.ApiException;
import com.arthium.finance.common.DateUtils;
import com.arthium.finance.common.MoneyUtils;
import com.arthium.finance.common.PaginationDto;
import com.arthium.finance.common.Values;
import com.arthium.finance.cron.DynamicJobScheduler;
import com.arthium.finance.report.dto.GeneratedReport;
import com.arthium.finance.report.dto.ReportListResponse;
import com.arthium.finance.report.dto.ReportResponse;
import com.arthium.finance.report.dto.ReportSettingUpdateRequest;
import com.arthium.finance.report.dto.ReportSummary;
import com.arthium.finance.report.dto.TopCategory;
import com.arthium.finance.transaction.TransactionType;
import com.arthium.finance.user.dto.ReportScheduleResponse;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ReportService {

    /** Daily budget threshold check at 09:00 in the user's timezone. */
    private static final String BUDGET_CRON = "0 9 * * *";

    private final MongoTemplate mongoTemplate;
    private final ReportRepository reportRepository;
    private final ReportSettingsRepository reportSettingsRepository;
    private final ReportScheduleRepository reportScheduleRepository;
    private final GeminiService geminiService;
    private final DynamicJobScheduler jobScheduler;

    public ReportService(MongoTemplate mongoTemplate,
                         ReportRepository reportRepository,
                         ReportSettingsRepository reportSettingsRepository,
                         ReportScheduleRepository reportScheduleRepository,
                         GeminiService geminiService,
                         DynamicJobScheduler jobScheduler) {
        this.mongoTemplate = mongoTemplate;
        this.reportRepository = reportRepository;
        this.reportSettingsRepository = reportSettingsRepository;
        this.reportScheduleRepository = reportScheduleRepository;
        this.geminiService = geminiService;
        this.jobScheduler = jobScheduler;
    }

    // ── Listing ──────────────────────────────────────────────────────────────

    public ReportListResponse getAllReports(int pageNumber, int pageSize, String userId) {
        PageRequest pageRequest = PageRequest.of(
                pageNumber - 1, pageSize, Sort.by(Sort.Direction.DESC, "created_at"));

        Page<Report> page = reportRepository.findByUserId(new ObjectId(userId), pageRequest);

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
        MongoCollection<Document> transactions = mongoTemplate.getCollection("transactions");

        List<Document> pipeline = List.of(
                new Document("$match", new Document("user_id", new ObjectId(userId))
                        .append("date", new Document("$gte", Date.from(fromDate)).append("$lte", Date.from(toDate)))),
                new Document("$facet", new Document()
                        .append("summary", List.of(
                                new Document("$group", new Document("_id", null)
                                        .append("total_income", sumWhenType(TransactionType.INCOME))
                                        .append("total_expense", sumWhenType(TransactionType.EXPENSE)))
                        ))
                        .append("categories", List.of(
                                new Document("$match", new Document("type", TransactionType.EXPENSE.name())),
                                new Document("$group", new Document("_id", "$category")
                                        .append("total", new Document("$sum", new Document("$abs", "$amount")))),
                                new Document("$sort", new Document("total", -1)),
                                new Document("$limit", 5)
                        ))),
                new Document("$project", new Document()
                        .append("total_income", new Document("$arrayElemAt", List.of("$summary.total_income", 0)))
                        .append("total_expenses", new Document("$arrayElemAt", List.of("$summary.total_expense", 0)))
                        .append("categories", 1))
        );

        Document result = transactions.aggregate(pipeline).first();

        long totalIncome = result == null ? 0 : Values.asLong(result.get("total_income"));
        long totalExpenses = result == null ? 0 : Values.asLong(result.get("total_expenses"));

        if (result == null || (totalIncome == 0 && totalExpenses == 0)) {
            return null;
        }

        List<?> rawCategories = result.get("categories") instanceof List<?> list ? list : List.of();

        // name -> {amount (dollars), percentage}
        Map<String, Map<String, Object>> byCategory = new LinkedHashMap<>();
        for (Object entry : rawCategories) {
            if (!(entry instanceof Document category)) {
                continue;
            }
            long total = Values.asLong(category.get("total"));
            long percentage = totalExpenses > 0 ? Math.round((total * 100.0) / totalExpenses) : 0;

            Map<String, Object> values = new LinkedHashMap<>();
            values.put("amount", MoneyUtils.toDollars(total));
            values.put("percentage", percentage);
            byCategory.put(Values.asString(category.get("_id")), values);
        }

        long availableBalance = totalIncome - totalExpenses;
        double savingsRate = MoneyUtils.savingRate(totalIncome, totalExpenses);
        String periodLabel = DateUtils.formatPeriodLabel(fromDate, toDate);

        List<String> insights = geminiService.generateAiInsights(
                totalIncome, totalExpenses, availableBalance, savingsRate, byCategory, periodLabel);

        List<TopCategory> topCategories = new ArrayList<>();
        byCategory.forEach((name, values) -> topCategories.add(new TopCategory(
                name,
                Values.asDouble(values.get("amount")),
                Values.asDouble(values.get("percentage"))
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

    private static Document sumWhenType(TransactionType type) {
        return new Document("$sum", new Document("$cond", List.of(
                new Document("$eq", List.of("$type", type.name())),
                new Document("$abs", "$amount"),
                0
        )));
    }

    // ── Settings ─────────────────────────────────────────────────────────────

    public void updateReportSetting(ReportSettingUpdateRequest request, String userId) {
        ReportSettings settings = reportSettingsRepository.findByUserId(new ObjectId(userId))
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
        return reportSettingsRepository.findByUserId(new ObjectId(userId));
    }

    // ── Scheduling ───────────────────────────────────────────────────────────

    public void scheduleReportJob(String userId, String timezone, String scheduledTime) {
        Instant now = Instant.now();
        ObjectId ownerId = new ObjectId(userId);

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
        ReportSchedule schedule = reportScheduleRepository.findByUserId(new ObjectId(userId))
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
