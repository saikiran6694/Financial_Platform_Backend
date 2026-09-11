package com.arthium.finance.budget;

import com.arthium.finance.budget.dto.BudgetCreateRequest;
import com.arthium.finance.budget.dto.BudgetItem;
import com.arthium.finance.budget.dto.BudgetListResponse;
import com.arthium.finance.budget.dto.BudgetUpdateRequest;
import com.arthium.finance.common.ApiException;
import com.arthium.finance.cron.DynamicJobScheduler;
import com.arthium.finance.report.UserScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private BudgetSpendCalculator spendCalculator;
    @Mock
    private DynamicJobScheduler jobScheduler;
    @Mock
    private UserScheduleService userScheduleService;

    private BudgetService budgetService;
    private final String userId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        budgetService = new BudgetService(budgetRepository, spendCalculator, jobScheduler, userScheduleService);
    }

    private static Budget budget(String category, long limitCents, boolean active) {
        Budget budget = new Budget();
        budget.setId(UUID.randomUUID());
        budget.setCategory(category);
        budget.setLimitAmount(limitCents);
        budget.setAlertThreshold(0.8);
        budget.setActive(active);
        return budget;
    }

    // ── create ───────────────────────────────────────────────────────────────

    @Test
    void create_duplicateCategoryCaseInsensitive_throwsConflict() {
        when(budgetRepository.existsByUserIdAndCategoryIgnoreCase(UUID.fromString(userId), "Food")).thenReturn(true);
        BudgetCreateRequest request = new BudgetCreateRequest("Food", 500.0, BudgetPeriod.MONTHLY, 0.8);

        assertThatThrownBy(() -> budgetService.create(request, userId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(budgetRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void create_success_convertsLimitToCentsAndSchedulesBudgetJob() {
        when(budgetRepository.existsByUserIdAndCategoryIgnoreCase(UUID.fromString(userId), "Food")).thenReturn(false);
        when(budgetRepository.save(any(Budget.class))).thenAnswer(inv -> {
            Budget b = inv.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });
        when(userScheduleService.getUserTimezone(userId)).thenReturn("America/St_Johns");
        when(spendCalculator.currentMonthSpendByCategory(userId)).thenReturn(Map.of());
        when(spendCalculator.spentFor(any(), eq("Food"))).thenReturn(0L);
        when(spendCalculator.buildItem(any(Budget.class), anyLong()))
                .thenReturn(new BudgetItem("id", "Food", 500.0, 0.0, 500.0, 0.0, BudgetStatus.ON_TRACK, 0.8, true, BudgetPeriod.MONTHLY));

        BudgetCreateRequest request = new BudgetCreateRequest("Food", 500.0, BudgetPeriod.MONTHLY, 0.8);
        BudgetItem result = budgetService.create(request, userId);

        assertThat(result.category()).isEqualTo("Food");

        ArgumentCaptor<Budget> captor = ArgumentCaptor.forClass(Budget.class);
        verify(budgetRepository).save(captor.capture());
        assertThat(captor.getValue().getLimitAmount()).isEqualTo(50000L);
        assertThat(captor.getValue().isActive()).isTrue();

        verify(jobScheduler).schedule("budget", "0 9 * * *", "America/St_Johns", userId);
    }

    // ── getAll ───────────────────────────────────────────────────────────────

    @Test
    void getAll_totalsOnlyIncludeActiveBudgets() {
        Budget active = budget("Food", 50000L, true);
        Budget inactive = budget("Travel", 100000L, false);
        when(budgetRepository.findByUserIdOrderByCreatedAtDesc(UUID.fromString(userId)))
                .thenReturn(List.of(active, inactive));
        when(spendCalculator.currentMonthSpendByCategory(userId)).thenReturn(Map.of("food", 20000L, "travel", 30000L));
        when(spendCalculator.spentFor(any(), eq("Food"))).thenReturn(20000L);
        when(spendCalculator.spentFor(any(), eq("Travel"))).thenReturn(30000L);
        when(spendCalculator.buildItem(eq(active), eq(20000L)))
                .thenReturn(new BudgetItem(active.getId().toString(), "Food", 500.0, 200.0, 300.0, 40.0, BudgetStatus.ON_TRACK, 0.8, true, BudgetPeriod.MONTHLY));
        when(spendCalculator.buildItem(eq(inactive), eq(30000L)))
                .thenReturn(new BudgetItem(inactive.getId().toString(), "Travel", 1000.0, 300.0, 700.0, 30.0, BudgetStatus.ON_TRACK, 0.8, false, BudgetPeriod.MONTHLY));

        BudgetListResponse response = budgetService.getAll(userId);

        assertThat(response.budgets()).hasSize(2);
        // Only the active budget's limit/spend contribute to the aggregate totals.
        assertThat(response.totalLimit()).isEqualTo(500.0);
        assertThat(response.totalSpent()).isEqualTo(200.0);
    }

    @Test
    void getAll_noBudgets_returnsZeroTotalsAndEmptyItems() {
        when(budgetRepository.findByUserIdOrderByCreatedAtDesc(UUID.fromString(userId))).thenReturn(List.of());
        when(spendCalculator.currentMonthSpendByCategory(userId)).thenReturn(Map.of());

        BudgetListResponse response = budgetService.getAll(userId);

        assertThat(response.budgets()).isEmpty();
        assertThat(response.totalLimit()).isEqualTo(0.0);
        assertThat(response.totalSpent()).isEqualTo(0.0);
    }

    // ── update ───────────────────────────────────────────────────────────────

    @Test
    void update_invalidBudgetId_throwsBadRequest() {
        BudgetUpdateRequest request = new BudgetUpdateRequest(600.0, null, null);

        assertThatThrownBy(() -> budgetService.update("not-an-id", request, userId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void update_budgetNotFound_throwsNotFound() {
        String budgetId = UUID.randomUUID().toString();
        when(budgetRepository.findByIdAndUserId(UUID.fromString(budgetId), UUID.fromString(userId)))
                .thenReturn(Optional.empty());
        BudgetUpdateRequest request = new BudgetUpdateRequest(600.0, null, null);

        assertThatThrownBy(() -> budgetService.update(budgetId, request, userId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Budget not found");
    }

    @Test
    void update_settingNewLimitAmount_resetsAlertHistory() {
        String budgetId = UUID.randomUUID().toString();
        Budget existing = budget("Food", 50000L, true);
        existing.setLastAlertedPeriod("2026-05");
        existing.setLastAlertedLevel("warning");
        when(budgetRepository.findByIdAndUserId(UUID.fromString(budgetId), UUID.fromString(userId)))
                .thenReturn(Optional.of(existing));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));
        when(spendCalculator.currentMonthSpendByCategory(userId)).thenReturn(Map.of());
        when(spendCalculator.spentFor(any(), any())).thenReturn(0L);
        when(spendCalculator.buildItem(any(Budget.class), anyLong()))
                .thenReturn(new BudgetItem(budgetId, "Food", 600.0, 0.0, 600.0, 0.0, BudgetStatus.ON_TRACK, 0.8, true, BudgetPeriod.MONTHLY));

        BudgetUpdateRequest request = new BudgetUpdateRequest(600.0, null, null);
        budgetService.update(budgetId, request, userId);

        assertThat(existing.getLimitAmount()).isEqualTo(60000L);
        assertThat(existing.getLastAlertedPeriod()).isNull();
        assertThat(existing.getLastAlertedLevel()).isNull();
    }

    @Test
    void update_onlyChangingAlertThreshold_doesNotResetAlertHistory() {
        String budgetId = UUID.randomUUID().toString();
        Budget existing = budget("Food", 50000L, true);
        existing.setLastAlertedPeriod("2026-05");
        existing.setLastAlertedLevel("warning");
        when(budgetRepository.findByIdAndUserId(UUID.fromString(budgetId), UUID.fromString(userId)))
                .thenReturn(Optional.of(existing));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));
        when(spendCalculator.currentMonthSpendByCategory(userId)).thenReturn(Map.of());
        when(spendCalculator.spentFor(any(), any())).thenReturn(0L);
        when(spendCalculator.buildItem(any(Budget.class), anyLong()))
                .thenReturn(new BudgetItem(budgetId, "Food", 500.0, 0.0, 500.0, 0.0, BudgetStatus.ON_TRACK, 0.5, true, BudgetPeriod.MONTHLY));

        BudgetUpdateRequest request = new BudgetUpdateRequest(null, 0.5, null);
        budgetService.update(budgetId, request, userId);

        assertThat(existing.getAlertThreshold()).isEqualTo(0.5);
        assertThat(existing.getLastAlertedPeriod()).isEqualTo("2026-05");
        assertThat(existing.getLastAlertedLevel()).isEqualTo("warning");
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    void delete_invalidBudgetId_throwsBadRequest() {
        assertThatThrownBy(() -> budgetService.delete("bad-id", userId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid budget id");
    }

    @Test
    void delete_budgetNotFound_throwsNotFound() {
        String budgetId = UUID.randomUUID().toString();
        when(budgetRepository.findByIdAndUserId(UUID.fromString(budgetId), UUID.fromString(userId)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.delete(budgetId, userId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Budget not found");
    }

    @Test
    void delete_success_removesBudgetAndReturnsId() {
        String budgetId = UUID.randomUUID().toString();
        Budget existing = budget("Food", 50000L, true);
        when(budgetRepository.findByIdAndUserId(UUID.fromString(budgetId), UUID.fromString(userId)))
                .thenReturn(Optional.of(existing));

        String result = budgetService.delete(budgetId, userId);

        assertThat(result).isEqualTo(budgetId);
        verify(budgetRepository).delete(existing);
    }
}
