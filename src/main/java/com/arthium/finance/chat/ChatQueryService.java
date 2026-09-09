package com.arthium.finance.chat;

import com.arthium.finance.budget.Budget;
import com.arthium.finance.budget.BudgetRepository;
import com.arthium.finance.budget.BudgetSpendCalculator;
import com.arthium.finance.budget.BudgetStatus;
import com.arthium.finance.common.DateUtils;
import com.arthium.finance.common.MoneyUtils;
import com.arthium.finance.common.Values;
import com.arthium.finance.transaction.Transaction;
import com.arthium.finance.transaction.TransactionType;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Port of services/chat_db.py — every MongoDB query the chat model can call.
 * Each method maps 1:1 to a tool. All amounts are returned in dollars.
 */
@Service
public class ChatQueryService {

    private final MongoTemplate mongoTemplate;
    private final BudgetRepository budgetRepository;
    private final BudgetSpendCalculator spendCalculator;

    public ChatQueryService(MongoTemplate mongoTemplate,
                            BudgetRepository budgetRepository,
                            BudgetSpendCalculator spendCalculator) {
        this.mongoTemplate = mongoTemplate;
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

        List<Document> pipeline = List.of(
                new Document("$match", match(userId, from, to, null)),
                new Document("$group", new Document("_id", null)
                        .append("total_income", sumWhenType(TransactionType.INCOME))
                        .append("total_expense", sumWhenType(TransactionType.EXPENSE))
                        .append("count", new Document("$sum", 1)))
        );

        Document result = transactions().aggregate(pipeline).first();

        long income = result == null ? 0 : Values.asLong(result.get("total_income"));
        long expense = result == null ? 0 : Values.asLong(result.get("total_expense"));
        long count = result == null ? 0 : Values.asLong(result.get("count"));
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

        List<Criteria> conditions = new ArrayList<>();
        conditions.add(Criteria.where("user_id").is(new ObjectId(userId)));

        if (from != null && to != null) {
            conditions.add(Criteria.where("date").gte(from).lte(to));
        }
        if (type != null && !type.isBlank()) {
            conditions.add(Criteria.where("type").is(type.toUpperCase(Locale.ENGLISH)));
        }
        if (category != null && !category.isBlank()) {
            conditions.add(Criteria.where("category").regex(category, "i"));
        }
        if (keyword != null && !keyword.isBlank()) {
            conditions.add(new Criteria().orOperator(
                    Criteria.where("title").regex(keyword, "i"),
                    Criteria.where("category").regex(keyword, "i")));
        }

        Sort sort = switch (sortBy == null ? "date_desc" : sortBy) {
            case "amount_desc" -> Sort.by(Sort.Direction.DESC, "amount");
            case "amount_asc" -> Sort.by(Sort.Direction.ASC, "amount");
            case "date_asc" -> Sort.by(Sort.Direction.ASC, "date");
            default -> Sort.by(Sort.Direction.DESC, "date");
        };

        Query query = new Query(new Criteria().andOperator(conditions.toArray(new Criteria[0])))
                .with(sort)
                .limit(cappedLimit);

        List<Map<String, Object>> results = new ArrayList<>();
        for (Transaction tx : mongoTemplate.find(query, Transaction.class)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", tx.getId() != null ? tx.getId().toHexString() : null);
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

        List<Document> pipeline = List.of(
                new Document("$match", match(userId, from, to, TransactionType.EXPENSE)),
                new Document("$group", new Document("_id", "$category")
                        .append("total", new Document("$sum", new Document("$abs", "$amount")))
                        .append("count", new Document("$sum", 1))),
                new Document("$sort", new Document("total", -1)),
                new Document("$limit", cappedLimit)
        );

        List<Document> rows = new ArrayList<>();
        for (Document row : transactions().aggregate(pipeline)) {
            rows.add(row);
        }

        long grandTotal = rows.stream().mapToLong(row -> Values.asLong(row.get("total"))).sum();

        List<Map<String, Object>> categories = new ArrayList<>();
        for (Document row : rows) {
            long total = Values.asLong(row.get("total"));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("category", Values.asString(row.get("_id")));
            item.put("amount", MoneyUtils.toDollars(total));
            item.put("percentage", grandTotal > 0 ? MoneyUtils.round((total * 100.0) / grandTotal, 1) : 0);
            item.put("transaction_count", Values.asLong(row.get("count")));
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
            case "day" -> "%Y-%m-%d";
            case "week" -> "%Y-W%V";
            default -> "%Y-%m";
        };

        List<Document> pipeline = List.of(
                new Document("$match", match(userId, from, to, null)),
                new Document("$group", new Document("_id",
                        new Document("$dateToString", new Document("format", format).append("date", "$date")))
                        .append("income", sumWhenType(TransactionType.INCOME))
                        .append("expenses", sumWhenType(TransactionType.EXPENSE))),
                new Document("$sort", new Document("_id", 1))
        );

        List<Map<String, Object>> series = new ArrayList<>();
        for (Document row : transactions().aggregate(pipeline)) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("period", Values.asString(row.get("_id")));
            if (includeIncome) {
                entry.put("income", MoneyUtils.toDollars(Values.asLong(row.get("income"))));
            }
            if (includeExpenses) {
                entry.put("expenses", MoneyUtils.toDollars(Values.asLong(row.get("expenses"))));
            }
            series.add(entry);
        }
        return series;
    }

    // ── query_recurring ──────────────────────────────────────────────────────

    public Map<String, Object> queryRecurring(String userId, String type) {
        List<Criteria> conditions = new ArrayList<>();
        conditions.add(Criteria.where("user_id").is(new ObjectId(userId)));
        conditions.add(Criteria.where("is_recurring").is(true));
        if (type != null && !type.isBlank()) {
            conditions.add(Criteria.where("type").is(type.toUpperCase(Locale.ENGLISH)));
        }

        Query query = new Query(new Criteria().andOperator(conditions.toArray(new Criteria[0])))
                .with(Sort.by(Sort.Direction.DESC, "amount"));

        List<Map<String, Object>> items = new ArrayList<>();
        long total = 0;

        for (Transaction tx : mongoTemplate.find(query, Transaction.class)) {
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

        List<Budget> budgets = budgetRepository.findByUserIdAndActiveTrue(new ObjectId(userId));

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

    private MongoCollection<Document> transactions() {
        return mongoTemplate.getCollection("transactions");
    }

    private static Document match(String userId, Instant from, Instant to, TransactionType type) {
        Document match = new Document("user_id", new ObjectId(userId));
        if (type != null) {
            match.append("type", type.name());
        }
        if (from != null && to != null) {
            match.append("date", new Document("$gte", Date.from(from)).append("$lte", Date.from(to)));
        }
        return match;
    }

    private static Document sumWhenType(TransactionType type) {
        return new Document("$sum", new Document("$cond", List.of(
                new Document("$eq", List.of("$type", type.name())),
                new Document("$abs", "$amount"),
                0
        )));
    }

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
