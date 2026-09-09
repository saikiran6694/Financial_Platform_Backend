package com.arthium.finance.budget;

import com.arthium.finance.budget.dto.BudgetItem;
import com.arthium.finance.common.DateUtils;
import com.arthium.finance.common.MoneyUtils;
import com.arthium.finance.common.Values;
import com.arthium.finance.report.UserScheduleService;
import com.arthium.finance.transaction.TransactionType;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of the spend computation in services/budgets.py.
 *
 * This is the single source of truth shared by the GET /api/budget/all route,
 * the chat query layer and the daily budget-check cron, so all three always
 * agree on the numbers.
 */
@Component
public class BudgetSpendCalculator {

    private final MongoTemplate mongoTemplate;
    private final UserScheduleService userScheduleService;

    public BudgetSpendCalculator(MongoTemplate mongoTemplate, UserScheduleService userScheduleService) {
        this.mongoTemplate = mongoTemplate;
        this.userScheduleService = userScheduleService;
    }

    /** "Now" in the timezone the user's cron jobs run in. */
    public ZonedDateTime nowInUserZone(String userId) {
        ZoneId zone = ZoneId.of(userScheduleService.getUserTimezone(userId));
        return ZonedDateTime.now(zone);
    }

    /**
     * {lowercased category -> spent in cents} for EXPENSE transactions in the
     * user's current local calendar month. Keys are lowercased so they line up
     * regardless of how the budget category was typed.
     */
    public Map<String, Long> currentMonthSpendByCategory(String userId) {
        ZonedDateTime nowLocal = nowInUserZone(userId);
        Instant fromDate = DateUtils.startOfMonth(nowLocal);
        Instant toDate = DateUtils.endOfMonth(nowLocal);

        MongoCollection<Document> transactions = mongoTemplate.getCollection("transactions");

        List<Document> pipeline = List.of(
                new Document("$match", new Document("user_id", new ObjectId(userId))
                        .append("type", TransactionType.EXPENSE.name())
                        .append("date", new Document("$gte", Date.from(fromDate)).append("$lte", Date.from(toDate)))),
                new Document("$group", new Document("_id", new Document("$toLower", "$category"))
                        .append("spent", new Document("$sum", new Document("$abs", "$amount"))))
        );

        Map<String, Long> spendByCategory = new HashMap<>();
        for (Document row : transactions.aggregate(pipeline)) {
            spendByCategory.put(Values.asString(row.get("_id")), Values.asLong(row.get("spent")));
        }
        return spendByCategory;
    }

    public long spentFor(Map<String, Long> spendMap, String category) {
        if (category == null) {
            return 0L;
        }
        return spendMap.getOrDefault(category.toLowerCase(), 0L);
    }

    public static BudgetStatus statusFor(double percentage, double thresholdPercentage) {
        if (percentage >= 100) {
            return BudgetStatus.EXCEEDED;
        }
        if (percentage >= thresholdPercentage) {
            return BudgetStatus.WARNING;
        }
        return BudgetStatus.ON_TRACK;
    }

    /** Port of _build_budget_item. */
    public BudgetItem buildItem(Budget budget, long spentCents) {
        long limitCents = budget.getLimitAmount();
        double thresholdPercentage = MoneyUtils.round(budget.getAlertThreshold() * 100, 2);
        double percentage = limitCents > 0
                ? MoneyUtils.round((spentCents * 100.0) / limitCents, 1)
                : 0.0;

        return new BudgetItem(
                budget.getId() != null ? budget.getId().toHexString() : null,
                budget.getCategory(),
                MoneyUtils.toDollars(limitCents),
                MoneyUtils.toDollars(spentCents),
                MoneyUtils.toDollars(limitCents - spentCents),
                percentage,
                statusFor(percentage, thresholdPercentage),
                budget.getAlertThreshold(),
                budget.isActive(),
                budget.getPeriod() != null ? budget.getPeriod() : BudgetPeriod.MONTHLY
        );
    }
}
