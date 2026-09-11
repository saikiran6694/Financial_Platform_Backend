package com.arthium.finance.analytics;

import com.arthium.finance.common.DateUtils;
import com.arthium.finance.common.MoneyUtils;
import com.arthium.finance.report.DateRange;
import com.arthium.finance.transaction.CategoryTotal;
import com.arthium.finance.transaction.PeriodTotal;
import com.arthium.finance.transaction.SummaryTotals;
import com.arthium.finance.transaction.TransactionRepository;
import com.arthium.finance.transaction.TransactionType;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AnalyticsService {

    private final TransactionRepository transactionRepository;

    public AnalyticsService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    // ── Summary ──────────────────────────────────────────────────────────────

    public Map<String, Object> summary(String userId, DateRange preset, Instant customFrom, Instant customTo) {
        DateRangeResolver.Resolved range = DateRangeResolver.resolve(preset, customFrom, customTo);

        Totals current = totalsFor(userId, range.from(), range.to());

        long availableBalance = current.income() - current.expense();
        double savingsPercentage = current.income() <= 0
                ? 0d
                : ((double) (current.income() - current.expense()) / current.income()) * 100;
        double expenseRatio = current.income() <= 0
                ? 0d
                : ((double) current.expense() / current.income()) * 100;

        Map<String, Object> percentageChange = emptyPercentageChange();

        if (range.from() != null && range.to() != null && range.value() != DateRange.ALL_TIME) {
            long period = Math.abs(ChronoUnit.DAYS.between(range.from(), range.to())) + 1;
            boolean yearly = range.value() == DateRange.LAST_YEAR || range.value() == DateRange.THIS_YEAR;

            Instant previousFrom = yearly
                    ? range.from().atZone(DateUtils.UTC).minusYears(1).toInstant()
                    : range.from().atZone(DateUtils.UTC).minusDays(period).toInstant();
            Instant previousTo = yearly
                    ? range.to().atZone(DateUtils.UTC).minusYears(1).toInstant()
                    : range.to().atZone(DateUtils.UTC).minusDays(period).toInstant();

            Totals previous = totalsFor(userId, previousFrom, previousTo);

            if (previous.transactionCount() > 0) {
                long previousBalance = previous.income() - previous.expense();

                Map<String, Object> previousValues = new LinkedHashMap<>();
                previousValues.put("income_amount", MoneyUtils.toDollars(previous.income()));
                previousValues.put("expense_amount", MoneyUtils.toDollars(previous.expense()));
                previousValues.put("balance_amount", MoneyUtils.toDollars(previousBalance));

                percentageChange = new LinkedHashMap<>();
                percentageChange.put("income", DateUtils.percentageChange(previous.income(), current.income()));
                percentageChange.put("expenses", DateUtils.percentageChange(previous.expense(), current.expense()));
                percentageChange.put("balance", DateUtils.percentageChange(previousBalance, availableBalance));
                percentageChange.put("prev_period_from", previousFrom);
                percentageChange.put("prev_period_to", previousTo);
                percentageChange.put("previous_values", previousValues);
            }
        }

        Map<String, Object> savingRate = new LinkedHashMap<>();
        savingRate.put("percentage", MoneyUtils.round(savingsPercentage, 2));
        savingRate.put("expense_ratio", MoneyUtils.round(expenseRatio, 2));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("available_balance", MoneyUtils.toDollars(availableBalance));
        stats.put("total_income", MoneyUtils.toDollars(current.income()));
        stats.put("total_expenses", MoneyUtils.toDollars(current.expense()));
        stats.put("saving_rate", savingRate);
        stats.put("transaction_count", current.transactionCount());
        stats.put("percentage_change", percentageChange);
        stats.put("preset", presetOf(range));
        return stats;
    }

    // ── Chart ────────────────────────────────────────────────────────────────

    public Map<String, Object> chart(String userId, DateRange preset, Instant customFrom, Instant customTo) {
        DateRangeResolver.Resolved range = DateRangeResolver.resolve(preset, customFrom, customTo);

        List<Map<String, Object>> chartData = new ArrayList<>();
        long totalIncomeCount = 0;
        long totalExpenseCount = 0;

        for (PeriodTotal row : transactionRepository.sumByPeriod(
                UUID.fromString(userId), range.from(), range.to(), "YYYY-MM-DD")) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", row.getPeriod());
            point.put("income", MoneyUtils.toDollars(row.getIncome()));
            point.put("expenses", MoneyUtils.toDollars(row.getExpenses()));
            chartData.add(point);

            totalIncomeCount += row.getIncomeCount();
            totalExpenseCount += row.getExpenseCount();
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("chart_data", chartData);
        stats.put("total_income_count", totalIncomeCount);
        stats.put("total_expense_count", totalExpenseCount);
        stats.put("preset", presetOf(range));
        return stats;
    }

    // ── Expense breakdown ────────────────────────────────────────────────────

    /**
     * Top three categories by spend, with everything else folded into a single
     * "others" bucket — the same shape the Python $facet pipeline produced.
     */
    public Map<String, Object> expenseBreakdown(String userId, DateRange preset, Instant customFrom, Instant customTo) {
        DateRangeResolver.Resolved range = DateRangeResolver.resolve(preset, customFrom, customTo);

        List<Map.Entry<String, Long>> categories = new ArrayList<>();
        for (CategoryTotal row : transactionRepository.sumByCategory(
                UUID.fromString(userId), TransactionType.EXPENSE.name(), range.from(), range.to())) {
            String name = row.getCategory();
            categories.add(Map.entry(name == null ? "" : name, row.getTotal()));
        }
        categories.sort(Comparator.comparingLong((Map.Entry<String, Long> e) -> e.getValue()).reversed());

        List<Map.Entry<String, Long>> buckets = new ArrayList<>(categories.subList(0, Math.min(3, categories.size())));
        if (categories.size() > 3) {
            long othersTotal = categories.subList(3, categories.size()).stream()
                    .mapToLong(Map.Entry::getValue)
                    .sum();
            buckets.add(Map.entry("others", othersTotal));
        }

        long totalSpent = buckets.stream().mapToLong(Map.Entry::getValue).sum();

        List<Map<String, Object>> breakdown = new ArrayList<>();
        for (Map.Entry<String, Long> bucket : buckets) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", bucket.getKey());
            item.put("value", MoneyUtils.toDollars(bucket.getValue()));
            item.put("percentage", totalSpent == 0
                    ? 0L
                    : Math.round((bucket.getValue() * 100.0) / totalSpent));
            breakdown.add(item);
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total_spent", MoneyUtils.toDollars(totalSpent));
        stats.put("breakdown", breakdown);
        stats.put("preset", presetOf(range));
        return stats;
    }

    // ── Shared plumbing ──────────────────────────────────────────────────────

    private record Totals(long income, long expense, long transactionCount) {
    }

    private Totals totalsFor(String userId, Instant fromDate, Instant toDate) {
        SummaryTotals result = transactionRepository.sumTotals(UUID.fromString(userId), fromDate, toDate);
        return new Totals(result.getTotalIncome(), result.getTotalExpense(), result.getTransactionCount());
    }

    private static Map<String, Object> presetOf(DateRangeResolver.Resolved range) {
        Map<String, Object> preset = new LinkedHashMap<>();
        preset.put("from", range.from());
        preset.put("to", range.to());
        preset.put("value", range.value());
        preset.put("label", range.label());
        return preset;
    }

    private static Map<String, Object> emptyPercentageChange() {
        Map<String, Object> previousValues = new LinkedHashMap<>();
        previousValues.put("income_amount", 0);
        previousValues.put("expense_amount", 0);
        previousValues.put("balance_amount", 0);

        Map<String, Object> percentageChange = new LinkedHashMap<>();
        percentageChange.put("income", 0);
        percentageChange.put("expenses", 0);
        percentageChange.put("balance", 0);
        percentageChange.put("prev_period_from", null);
        percentageChange.put("prev_period_to", null);
        percentageChange.put("previous_values", previousValues);
        return percentageChange;
    }
}
