package com.arthium.finance.budget;

import com.arthium.finance.budget.dto.BudgetItem;
import com.arthium.finance.common.DateUtils;
import com.arthium.finance.common.MoneyUtils;
import com.arthium.finance.report.UserScheduleService;
import com.arthium.finance.transaction.CategoryTotal;
import com.arthium.finance.transaction.TransactionRepository;
import com.arthium.finance.transaction.TransactionType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class BudgetSpendCalculator {

    private final TransactionRepository transactionRepository;
    private final UserScheduleService userScheduleService;

    public BudgetSpendCalculator(TransactionRepository transactionRepository, UserScheduleService userScheduleService) {
        this.transactionRepository = transactionRepository;
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

        Map<String, Long> spendByCategory = new HashMap<>();
        for (CategoryTotal row : transactionRepository.sumByLowerCategory(
                UUID.fromString(userId), TransactionType.EXPENSE.name(), fromDate, toDate)) {
            spendByCategory.put(row.getCategory(), row.getTotal());
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
                budget.getId() != null ? budget.getId().toString() : null,
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
