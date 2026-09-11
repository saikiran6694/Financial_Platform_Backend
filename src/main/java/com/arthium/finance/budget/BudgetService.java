package com.arthium.finance.budget;

import com.arthium.finance.budget.dto.BudgetCreateRequest;
import com.arthium.finance.budget.dto.BudgetItem;
import com.arthium.finance.budget.dto.BudgetListResponse;
import com.arthium.finance.budget.dto.BudgetUpdateRequest;
import com.arthium.finance.common.ApiException;
import com.arthium.finance.common.Ids;
import com.arthium.finance.common.MoneyUtils;
import com.arthium.finance.cron.DynamicJobScheduler;
import com.arthium.finance.report.UserScheduleService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BudgetService {

    private static final String BUDGET_CRON = "0 9 * * *";

    private final BudgetRepository budgetRepository;
    private final BudgetSpendCalculator spendCalculator;
    private final DynamicJobScheduler jobScheduler;
    private final UserScheduleService userScheduleService;

    public BudgetService(BudgetRepository budgetRepository,
                         BudgetSpendCalculator spendCalculator,
                         DynamicJobScheduler jobScheduler,
                         UserScheduleService userScheduleService) {
        this.budgetRepository = budgetRepository;
        this.spendCalculator = spendCalculator;
        this.jobScheduler = jobScheduler;
        this.userScheduleService = userScheduleService;
    }

    public BudgetItem create(BudgetCreateRequest request, String userId) {
        UUID ownerId = UUID.fromString(userId);

        boolean exists = budgetRepository.existsByUserIdAndCategoryIgnoreCase(ownerId, request.category());

        if (exists) {
            throw ApiException.conflict(
                    "A budget for category '" + request.category() + "' already exists.");
        }

        Instant now = Instant.now();

        Budget budget = new Budget();
        budget.setUserId(ownerId);
        budget.setCategory(request.category());
        budget.setLimitAmount(MoneyUtils.toCents(request.limitAmount()));
        budget.setPeriod(request.period());
        budget.setAlertThreshold(request.alertThreshold());
        budget.setActive(true);
        budget.setLastAlertedPeriod(null);
        budget.setLastAlertedLevel(null);
        budget.setCreatedAt(now);
        budget.setUpdatedAt(now);

        Budget created = budgetRepository.save(budget);

        ensureBudgetJobScheduled(userId);

        Map<String, Long> spendMap = spendCalculator.currentMonthSpendByCategory(userId);
        return spendCalculator.buildItem(created, spendCalculator.spentFor(spendMap, created.getCategory()));
    }

    public BudgetListResponse getAll(String userId) {
        List<Budget> budgets = budgetRepository.findByUserIdOrderByCreatedAtDesc(UUID.fromString(userId));
        Map<String, Long> spendMap = spendCalculator.currentMonthSpendByCategory(userId);

        List<BudgetItem> items = new ArrayList<>();
        long totalLimit = 0;
        long totalSpent = 0;

        for (Budget budget : budgets) {
            long spent = spendCalculator.spentFor(spendMap, budget.getCategory());
            items.add(spendCalculator.buildItem(budget, spent));
            if (budget.isActive()) {
                totalLimit += budget.getLimitAmount();
                totalSpent += spent;
            }
        }

        return new BudgetListResponse(
                "Budgets fetched successfully",
                MoneyUtils.toDollars(totalLimit),
                MoneyUtils.toDollars(totalSpent),
                items
        );
    }

    public BudgetItem update(String budgetId, BudgetUpdateRequest request, String userId) {
        Budget budget = findOwned(budgetId, userId);

        if (request.limitAmount() != null) {
            budget.setLimitAmount(MoneyUtils.toCents(request.limitAmount()));
            // A new limit means the user should be able to be alerted again this period.
            budget.setLastAlertedPeriod(null);
            budget.setLastAlertedLevel(null);
        }
        if (request.alertThreshold() != null) {
            budget.setAlertThreshold(request.alertThreshold());
        }
        if (request.isActive() != null) {
            budget.setActive(request.isActive());
        }
        budget.setUpdatedAt(Instant.now());

        Budget updated = budgetRepository.save(budget);

        Map<String, Long> spendMap = spendCalculator.currentMonthSpendByCategory(userId);
        return spendCalculator.buildItem(updated, spendCalculator.spentFor(spendMap, updated.getCategory()));
    }

    public String delete(String budgetId, String userId) {
        Budget budget = findOwned(budgetId, userId);
        budgetRepository.delete(budget);
        return budgetId;
    }

    private Budget findOwned(String budgetId, String userId) {
        if (!Ids.isValid(budgetId)) {
            throw ApiException.badRequest("Invalid budget id");
        }
        return budgetRepository.findByIdAndUserId(UUID.fromString(budgetId), UUID.fromString(userId))
                .orElseThrow(() -> ApiException.notFound("Budget not found"));
    }

    /**
     * Port of _ensure_budget_job_scheduled.
     *
     * Creating a budget is the one path where a user can end up with budgets
     * but no cron job — the budget job is otherwise only registered as a side
     * effect of setting up a report schedule, or at startup from existing
     * scheduler documents. Without this they would never receive alerts.
     */
    private void ensureBudgetJobScheduled(String userId) {
        String timezone = userScheduleService.getUserTimezone(userId);
        jobScheduler.schedule("budget", BUDGET_CRON, timezone, userId);
    }
}
