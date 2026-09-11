package com.arthium.finance.chat;

import com.arthium.finance.budget.Budget;
import com.arthium.finance.budget.BudgetRepository;
import com.arthium.finance.budget.BudgetSpendCalculator;
import com.arthium.finance.budget.BudgetStatus;
import com.arthium.finance.common.DateUtils;
import com.arthium.finance.common.MoneyUtils;
import com.arthium.finance.transaction.CategoryTotal;
import com.arthium.finance.transaction.SummaryTotals;
import com.arthium.finance.transaction.Transaction;
import com.arthium.finance.transaction.TransactionRepository;
import com.arthium.finance.transaction.TransactionSpecifications;
import com.arthium.finance.transaction.TransactionType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;


@Service
public class ChatQueryService {

    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final BudgetSpendCalculator spendCalculator;

    public ChatQueryService(TransactionRepository transactionRepository,
                            BudgetRepository budgetRepository,
                            BudgetSpendCalculator spendCalculator) {
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.spendCalculator = spendCalculator;
    }

    /** Dispatches a tool call by name. */
    public Object execute(String toolName, String userId, Map<String, Object> args) {
        return switch (toolName) {
            case "query_summary" -> querySummary(userId, str(args, "from_date"), str(args, "to_date"));
            case "query_transactions" -> queryTransactions(
                    userId,
                    str(args, "from_date"),
                    str(args, "to_date"),
                    str(args, "type"),
                    str(args, "category"),
                    str(args, "keyword"),
                    str(args, "sort_by"),
                    integer(args, "limit", 10));
            case "query_expense_breakdown" -> queryExpenseBreakdown(
                    userId, str(args, "from_date"), str(args, "to_date"), integer(args, "limit", 10));
            case "query_time_series" -> queryTimeSeries(
                    userId,
                    str(args, "from_date"),
                    str(args, "to_date"),
                    str(args, "granularity"),
                    bool(args, "include_income", true),
                    bool(args, "include_expenses", true));
            case "query_recurring" -> queryRecurring(userId, str(args, "type"));
            case "query_period_comparison" -> queryPeriodComparison(
                    userId,
                    str(args, "period_a_from"),
                    str(args, "period_a_to"),
                    str(args, "period_b_from"),
                    str(args, "period_b_to"),
                    str(args, "period_a_label") != null ? str(args, "period_a_label") : "Period A",
                    str(args, "period_b_label") != null ? str(args, "period_b_label") : "Period B");
            case "query_budget_status" -> queryBudgetStatus(userId, str(args, "category"));
            default -> Map.of("error", "Unknown tool: " + toolName);
        };
    }

    // ── query_summary ────────────────────────────────────────────────────────

    public Map<String, Object> querySummary(String userId, String fromDate, String toDate) {
        Instant from = parseDate(fromDate);
        Instant to = parseDate(toDate);

        SummaryTotals result = transactionRepository.sumTotals(UUID.fromString(userId), from, to);

        long income = result.getTotalIncome();
        long expense = result.getTotalExpense();
        long count = result.getTransactionCount();
        long balance = income - expense;

        Map<String, Object> period = new LinkedHashMap<>();
        period.put("from", fromDate);
        period.put("to", toDate);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total_income", MoneyUtils.toDollars(income));
        summary.put("total_expenses", MoneyUtils.toDollars(expense));
        summary.put("available_balance", MoneyUtils.toDollars(balance));
        summary.put("savings_rate", MoneyUtils.savingRate(income, expense));
        summary.put("transaction_count", count);
        summary.put("period", period);
        return summary;
    }

    // ── query_transactions ───────────────────────────────────────────────────

    public List<Map<String, Object>> queryTransactions(String userId,
                                                       String fromDate,
                                                       String toDate,
                                                       String type,
                                                       String category,
                                                       String keyword,
                                                       String sortBy,
                                                       int limit) {
        Instant from = parseDate(fromDate);
        Instant to = parseDate(toDate);
        int cappedLimit = Math.max(1, Math.min(limit, 50));

        TransactionType typeFilter = (type != null && !type.isBlank())
                ? TransactionType.valueOf(type.toUpperCase(Locale.ENGLISH))
                : null;

        Specification<Transaction> spec = TransactionSpecifications.combine(
                TransactionSpecifications.userIdEquals(UUID.fromString(userId)),
                TransactionSpecifications.dateBetween(from, to),
                TransactionSpecifications.typeEquals(typeFilter),
                TransactionSpecifications.categoryContainsIgnoreCase(category),
                TransactionSpecifications.titleOrCategoryContainsIgnoreCase(keyword));

        Sort sort = switch (sortBy == null ? "date_desc" : sortBy) {
            case "amount_desc" -> Sort.by(Sort.Direction.DESC, "amount");
            case "amount_asc" -> Sort.by(Sort.Direction.ASC, "amount");
            case "date_asc" -> Sort.by(Sort.Direction.ASC, "date");
            default -> Sort.by(Sort.Direction.DESC, "date");
        };

        List<Map<String, Object>> results = new ArrayList<>();
        for (Transaction tx : transactionRepository.findAll(spec, PageRequest.of(0, cappedLimit, sort))) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", tx.getId() != null ? tx.getId().toString() : null);
            item.put("title", tx.getTitle());
            item.put("amount", MoneyUtils.toDollars(tx.getAmount()));
            item.put("type", tx.getType() != null ? tx.getType().name() : null);
            item.put("category", tx.getCategory());
            item.put("date", tx.getDate() != null ? tx.getDate().toString() : null);
            item.put("payment_method", tx.getPaymentMethod() != null ? tx.getPaymentMethod().name() : null);
            item.put("is_recurring", tx.isRecurring());
            item.put("description", tx.getDescription());
            results.add(item);
        }
        return results;
    }

    // ── query_expense_breakdown ──────────────────────────────────────────────

    public Map<String, Object> queryExpenseBreakdown(String userId, String fromDate, String toDate, int limit) {
        Instant from = parseDate(fromDate);
        Instant to = parseDate(toDate);
        int cappedLimit = Math.max(1, Math.min(limit, 20));

        List<CategoryTotal> rows = transactionRepository.sumByCategory(
                UUID.fromString(userId), TransactionType.EXPENSE.name(), from, to)
                .stream().limit(cappedLimit).toList();

        long grandTotal = rows.stream().mapToLong(CategoryTotal::getTotal).sum();

        List<Map<String, Object>> categories = new ArrayList<>();
        for (CategoryTotal row : rows) {
            long total = row.getTotal();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("category", row.getCategory());
            item.put("amount", MoneyUtils.toDollars(total));
            item.put("percentage", grandTotal > 0 ? MoneyUtils.round((total * 100.0) / grandTotal, 1) : 0);
            item.put("transaction_count", row.getCount());
            categories.add(item);
        }

        Map<String, Object> period = new LinkedHashMap<>();
        period.put("from", fromDate);
        period.put("to", toDate);

        Map<String, Object> breakdown = new LinkedHashMap<>();
        breakdown.put("total_spent", MoneyUtils.toDollars(grandTotal));
        breakdown.put("categories", categories);
        breakdown.put("period", period);
        return breakdown;
    }

    // ── query_time_series ────────────────────────────────────────────────────

    public List<Map<String, Object>> queryTimeSeries(String userId,
                                                     String fromDate,
                                                     String toDate,
                                                     String granularity,
                                                     boolean includeIncome,
                                                     boolean includeExpenses) {
        Instant from = parseDate(fromDate);
        Instant to = parseDate(toDate);

        if (from == null || to == null) {
            return List.of();
        }

        String format = switch (granularity == null ? "month" : granularity) {
            case "day" -> "YYYY-MM-DD";
            case "week" -> "IYYY-\"W\"IW";
            default -> "YYYY-MM";
        };

        List<Map<String, Object>> series = new ArrayList<>();
        for (var row : transactionRepository.sumByPeriod(UUID.fromString(userId), from, to, format)) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("period", row.getPeriod());
            if (includeIncome) {
                entry.put("income", MoneyUtils.toDollars(row.getIncome()));
            }
            if (includeExpenses) {
                entry.put("expenses", MoneyUtils.toDollars(row.getExpenses()));
            }
            series.add(entry);
        }
        return series;
    }

    // ── query_recurring ──────────────────────────────────────────────────────

    public Map<String, Object> queryRecurring(String userId, String type) {
        TransactionType typeFilter = (type != null && !type.isBlank())
                ? TransactionType.valueOf(type.toUpperCase(Locale.ENGLISH))
                : null;

        Specification<Transaction> spec = TransactionSpecifications.combine(
                TransactionSpecifications.userIdEquals(UUID.fromString(userId)),
                TransactionSpecifications.recurringEquals(true),
                TransactionSpecifications.typeEquals(typeFilter));

        List<Map<String, Object>> items = new ArrayList<>();
        long total = 0;

        for (Transaction tx : transactionRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "amount"))) {
            total += tx.getAmount();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("title", tx.getTitle());
            item.put("amount", MoneyUtils.toDollars(tx.getAmount()));
            item.put("type", tx.getType() != null ? tx.getType().name() : null);
            item.put("category", tx.getCategory());
            item.put("recurring_interval",
                    tx.getRecurringInterval() != null ? tx.getRecurringInterval().name() : null);
            item.put("next_recurring_date",
                    tx.getNextRecurringDate() != null ? tx.getNextRecurringDate().toString() : null);
            items.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("recurring_transactions", items);
        result.put("total_recurring_amount", MoneyUtils.toDollars(total));
        result.put("count", items.size());
        return result;
    }

    // ── query_period_comparison ──────────────────────────────────────────────

    public Map<String, Object> queryPeriodComparison(String userId,
                                                     String periodAFrom,
                                                     String periodATo,
                                                     String periodBFrom,
                                                     String periodBTo,
                                                     String periodALabel,
                                                     String periodBLabel) {

        Map<String, Object> a = querySummary(userId, periodAFrom, periodATo);
        Map<String, Object> b = querySummary(userId, periodBFrom, periodBTo);

        Map<String, Object> periodA = new LinkedHashMap<>(a);
        periodA.put("label", periodALabel);

        Map<String, Object> periodB = new LinkedHashMap<>(b);
        periodB.put("label", periodBLabel);

        Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("income", percentChange(number(b, "total_income"), number(a, "total_income")));
        changes.put("expenses", percentChange(number(b, "total_expenses"), number(a, "total_expenses")));
        changes.put("balance", percentChange(number(b, "available_balance"), number(a, "available_balance")));
        changes.put("savings_rate",
                MoneyUtils.round(number(a, "savings_rate") - number(b, "savings_rate"), 2));

        Map<String, Object> comparison = new LinkedHashMap<>();
        comparison.put("period_a", periodA);
        comparison.put("period_b", periodB);
        comparison.put("changes", changes);
        return comparison;
    }

    // ── query_budget_status ──────────────────────────────────────────────────

    public Map<String, Object> queryBudgetStatus(String userId, String category) {
        ZonedDateTime now = spendCalculator.nowInUserZone(userId);
        String periodLabel = now.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));

        List<Budget> budgets = budgetRepository.findByUserIdAndActiveTrue(UUID.fromString(userId));

        if (category != null && !category.isBlank()) {
            budgets = budgets.stream()
                    .filter(budget -> budget.getCategory() != null
                            && budget.getCategory().equalsIgnoreCase(category.trim()))
                    .toList();
        }

        if (budgets.isEmpty()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("period", periodLabel);
            empty.put("total_limit", 0);
            empty.put("total_spent", 0);
            empty.put("budgets", List.of());
            empty.put("note", (category == null || category.isBlank())
                    ? "No active budgets set."
                    : "No active budget for '" + category + "'.");
            return empty;
        }

        Map<String, Long> spendMap = spendCalculator.currentMonthSpendByCategory(userId);

        List<Map<String, Object>> items = new ArrayList<>();
        long totalLimit = 0;
        long totalSpent = 0;

        for (Budget budget : budgets) {
            long spentCents = spendCalculator.spentFor(spendMap, budget.getCategory());
            long limitCents = budget.getLimitAmount();
            double thresholdPercentage = MoneyUtils.round(budget.getAlertThreshold() * 100, 2);
            double percentage = limitCents > 0
                    ? MoneyUtils.round((spentCents * 100.0) / limitCents, 1)
                    : 0.0;

            BudgetStatus status = BudgetSpendCalculator.statusFor(percentage, thresholdPercentage);

            totalLimit += limitCents;
            totalSpent += spentCents;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("category", budget.getCategory());
            item.put("limit", MoneyUtils.toDollars(limitCents));
            item.put("spent", MoneyUtils.toDollars(spentCents));
            item.put("remaining", MoneyUtils.toDollars(limitCents - spentCents));
            item.put("percentage_used", percentage);
            item.put("status", status.getValue());
            items.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period", periodLabel);
        result.put("total_limit", MoneyUtils.toDollars(totalLimit));
        result.put("total_spent", MoneyUtils.toDollars(totalSpent));
        result.put("budgets", items);
        return result;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static double percentChange(double previous, double current) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return MoneyUtils.round(((current - previous) / Math.abs(previous)) * 100, 1);
    }

    private static double number(Map<String, Object> source, String key) {
        Object value = source.get(key);
        return value instanceof Number n ? n.doubleValue() : 0d;
    }

    /** Port of _parse_date: lenient ISO-8601 parsing, null on failure. */
    static Instant parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalised = raw.trim();
        try {
            return Instant.parse(normalised);
        } catch (Exception ignored) {
            // fall through
        }
        try {
            return OffsetDateTime.parse(normalised).toInstant();
        } catch (Exception ignored) {
            // fall through
        }
        try {
            return java.time.LocalDateTime.parse(normalised).atZone(DateUtils.UTC).toInstant();
        } catch (Exception ignored) {
            // fall through
        }
        try {
            return java.time.LocalDate.parse(normalised).atStartOfDay(DateUtils.UTC).toInstant();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String str(Map<String, Object> args, String key) {
        Object value = args.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() || "null".equalsIgnoreCase(text) ? null : text;
    }

    private static int integer(Map<String, Object> args, String key, int fallback) {
        Object value = args.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean bool(Map<String, Object> args, String key, boolean fallback) {
        Object value = args.get(key);
        if (value instanceof Boolean b) {
            return b;
        }
        if (value == null) {
            return fallback;
        }
        return "true".equalsIgnoreCase(String.valueOf(value).trim());
    }
}
