package com.arthium.finance.budget;

import com.arthium.finance.budget.dto.BudgetItem;
import com.arthium.finance.report.UserScheduleService;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetSpendCalculatorTest {

    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private UserScheduleService userScheduleService;

    private BudgetSpendCalculator calculator;
    private final String userId = new ObjectId().toHexString();

    @BeforeEach
    void setUp() {
        calculator = new BudgetSpendCalculator(mongoTemplate, userScheduleService);
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
        budget.setId(new ObjectId());
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
        budget.setId(new ObjectId());
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
        budget.setId(new ObjectId());
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
    @SuppressWarnings("unchecked")
    void currentMonthSpendByCategory_aggregatesSpendGroupedByLowercasedCategory() {
        when(userScheduleService.getUserTimezone(userId)).thenReturn("UTC");

        MongoCollection<Document> collection = org.mockito.Mockito.mock(MongoCollection.class);
        AggregateIterable<Document> aggregateIterable = org.mockito.Mockito.mock(AggregateIterable.class);
        com.mongodb.client.MongoCursor<Document> cursor = org.mockito.Mockito.mock(com.mongodb.client.MongoCursor.class);
        Document row = new Document("_id", "food").append("spent", 5000L);
        when(cursor.hasNext()).thenReturn(true, false);
        when(cursor.next()).thenReturn(row);

        when(mongoTemplate.getCollection("transactions")).thenReturn(collection);
        when(collection.aggregate(anyList())).thenReturn(aggregateIterable);
        when(aggregateIterable.iterator()).thenReturn(cursor);

        Map<String, Long> result = calculator.currentMonthSpendByCategory(userId);

        assertThat(result).containsEntry("food", 5000L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void currentMonthSpendByCategory_noExpenses_returnsEmptyMap() {
        when(userScheduleService.getUserTimezone(userId)).thenReturn("UTC");

        MongoCollection<Document> collection = org.mockito.Mockito.mock(MongoCollection.class);
        AggregateIterable<Document> aggregateIterable = org.mockito.Mockito.mock(AggregateIterable.class);
        com.mongodb.client.MongoCursor<Document> cursor = org.mockito.Mockito.mock(com.mongodb.client.MongoCursor.class);
        when(cursor.hasNext()).thenReturn(false);

        when(mongoTemplate.getCollection("transactions")).thenReturn(collection);
        when(collection.aggregate(anyList())).thenReturn(aggregateIterable);
        when(aggregateIterable.iterator()).thenReturn(cursor);

        Map<String, Long> result = calculator.currentMonthSpendByCategory(userId);

        assertThat(result).isEmpty();
    }
}
