package com.arthium.finance.budget;

import com.arthium.finance.budget.dto.BudgetItem;
import com.arthium.finance.report.UserScheduleService;
import com.arthium.finance.transaction.CategoryTotal;
import com.arthium.finance.transaction.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetSpendCalculatorTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserScheduleService userScheduleService;

    private BudgetSpendCalculator calculator;
    private final String userId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        calculator = new BudgetSpendCalculator(transactionRepository, userScheduleService);
    }

    private record CategoryTotalRow(String category, long total, long count) implements CategoryTotal {
        public String getCategory() { return category; }
        public Long getTotal() { return total; }
        public Long getCount() { return count; }
    }

    // ── statusFor ────────────────────────────────────────────────────────────

    @Test
    void statusFor_percentageAtOrAbove100_returnsExceeded() {
        assertThat(BudgetSpendCalculator.statusFor(100.0, 80.0)).isEqualTo(BudgetStatus.EXCEEDED);
        assertThat(BudgetSpendCalculator.statusFor(150.0, 80.0)).isEqualTo(BudgetStatus.EXCEEDED);
    }

    @Test
    void statusFor_percentageAtOrAboveThresholdButBelow100_returnsWarning() {
        assertThat(BudgetSpendCalculator.statusFor(80.0, 80.0)).isEqualTo(BudgetStatus.WARNING);
        assertThat(BudgetSpendCalculator.statusFor(90.0, 80.0)).isEqualTo(BudgetStatus.WARNING);
    }

    @Test
    void statusFor_percentageBelowThreshold_returnsOnTrack() {
        assertThat(BudgetSpendCalculator.statusFor(79.9, 80.0)).isEqualTo(BudgetStatus.ON_TRACK);
    }

    // ── spentFor ─────────────────────────────────────────────────────────────

    @Test
    void spentFor_nullCategory_returnsZero() {
        assertThat(calculator.spentFor(Map.of("food", 500L), null)).isEqualTo(0L);
    }

    @Test
    void spentFor_categoryNotInMap_returnsZero() {
        assertThat(calculator.spentFor(Map.of("food", 500L), "travel")).isEqualTo(0L);
    }

    @Test
    void spentFor_categoryPresentRegardlessOfCase_returnsMatchingSpend() {
        assertThat(calculator.spentFor(Map.of("food", 500L), "FOOD")).isEqualTo(500L);
    }

    // ── buildItem ────────────────────────────────────────────────────────────

    @Test
    void buildItem_zeroLimit_avoidsDivideByZeroAndReturnsZeroPercentage() {
        Budget budget = new Budget();
        budget.setId(UUID.randomUUID());
        budget.setCategory("Food");
        budget.setLimitAmount(0L);
        budget.setAlertThreshold(0.8);
        budget.setActive(true);

        BudgetItem item = calculator.buildItem(budget, 500L);

        assertThat(item.percentageUsed()).isEqualTo(0.0);
        assertThat(item.status()).isEqualTo(BudgetStatus.ON_TRACK);
    }

    @Test
    void buildItem_normalCase_computesPercentageAndStatus() {
        Budget budget = new Budget();
        budget.setId(UUID.randomUUID());
        budget.setCategory("Food");
        budget.setLimitAmount(10000L);
        budget.setAlertThreshold(0.8);
        budget.setActive(true);

        BudgetItem item = calculator.buildItem(budget, 9000L);

        assertThat(item.percentageUsed()).isEqualTo(90.0);
        assertThat(item.status()).isEqualTo(BudgetStatus.WARNING);
        assertThat(item.remaining()).isEqualTo(10.0);
    }

    @Test
    void buildItem_nullPeriodOnEntity_defaultsToMonthly() {
        Budget budget = new Budget();
        budget.setId(UUID.randomUUID());
        budget.setCategory("Food");
        budget.setLimitAmount(1000L);
        budget.setAlertThreshold(0.8);
        budget.setPeriod(null);

        BudgetItem item = calculator.buildItem(budget, 100L);

        assertThat(item.period()).isEqualTo(BudgetPeriod.MONTHLY);
    }

    // ── nowInUserZone ────────────────────────────────────────────────────────

    @Test
    void nowInUserZone_invalidTimezone_throwsException() {
        when(userScheduleService.getUserTimezone(userId)).thenReturn("Not/AZone");

        assertThatThrownBy(() -> calculator.nowInUserZone(userId))
                .isInstanceOf(java.time.DateTimeException.class);
    }

    @Test
    void nowInUserZone_validTimezone_returnsCurrentTimeInThatZone() {
        when(userScheduleService.getUserTimezone(userId)).thenReturn("UTC");

        ZonedDateTime result = calculator.nowInUserZone(userId);

        assertThat(result.getZone()).isEqualTo(ZoneId.of("UTC"));
    }

    // ── currentMonthSpendByCategory ──────────────────────────────────────────

    @Test
    void currentMonthSpendByCategory_aggregatesSpendGroupedByLowercasedCategory() {
        when(userScheduleService.getUserTimezone(userId)).thenReturn("UTC");
        when(transactionRepository.sumByLowerCategory(eq(UUID.fromString(userId)), anyString(), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(new CategoryTotalRow("food", 5000L, 3L)));

        Map<String, Long> result = calculator.currentMonthSpendByCategory(userId);

        assertThat(result).containsEntry("food", 5000L);
    }

    @Test
    void currentMonthSpendByCategory_noExpenses_returnsEmptyMap() {
        when(userScheduleService.getUserTimezone(userId)).thenReturn("UTC");
        when(transactionRepository.sumByLowerCategory(eq(UUID.fromString(userId)), anyString(), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        Map<String, Long> result = calculator.currentMonthSpendByCategory(userId);

        assertThat(result).isEmpty();
    }
}
