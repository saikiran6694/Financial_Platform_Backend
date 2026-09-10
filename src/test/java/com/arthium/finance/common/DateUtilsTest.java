package com.arthium.finance.common;

import com.arthium.finance.transaction.RecurringInterval;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DateUtilsTest {

    @Test
    void nextReportDate_advancesToFirstOfFollowingMonth() {
        Instant lastSent = Instant.parse("2026-05-15T10:30:00Z");

        Instant result = DateUtils.nextReportDate(lastSent);

        assertThat(result).isEqualTo(Instant.parse("2026-06-01T00:00:00Z"));
    }

    @Test
    void nextReportDate_rollsOverFromDecemberToJanuary() {
        Instant lastSent = Instant.parse("2025-12-15T10:30:00Z");

        Instant result = DateUtils.nextReportDate(lastSent);

        assertThat(result).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void nextReportDate_withNullLastSentDate_usesNowAsBase() {
        Instant result = DateUtils.nextReportDate(null);

        ZonedDateTime resultZoned = result.atZone(DateUtils.UTC);
        assertThat(resultZoned.getDayOfMonth()).isEqualTo(1);
        assertThat(resultZoned.getHour()).isZero();
        assertThat(resultZoned.getMinute()).isZero();
        assertThat(resultZoned.getSecond()).isZero();
        assertThat(resultZoned.getNano()).isZero();
        assertThat(result).isAfter(Instant.now());
    }

    @Test
    void nextOccurrence_daily_advancesOneDayFromMidnight() {
        Instant date = Instant.parse("2026-03-15T13:45:30Z");

        Instant result = DateUtils.nextOccurrence(date, RecurringInterval.DAILY);

        assertThat(result).isEqualTo(Instant.parse("2026-03-16T00:00:00Z"));
    }

    @Test
    void nextOccurrence_weekly_advancesSevenDaysFromMidnight() {
        Instant date = Instant.parse("2026-03-15T13:45:30Z");

        Instant result = DateUtils.nextOccurrence(date, RecurringInterval.WEEKLY);

        assertThat(result).isEqualTo(Instant.parse("2026-03-22T00:00:00Z"));
    }

    @Test
    void nextOccurrence_monthly_advancesOneMonthFromMidnight() {
        Instant date = Instant.parse("2026-03-15T13:45:30Z");

        Instant result = DateUtils.nextOccurrence(date, RecurringInterval.MONTHLY);

        assertThat(result).isEqualTo(Instant.parse("2026-04-15T00:00:00Z"));
    }

    @Test
    void nextOccurrence_yearly_advancesOneYearFromMidnight() {
        Instant date = Instant.parse("2026-03-15T13:45:30Z");

        Instant result = DateUtils.nextOccurrence(date, RecurringInterval.YEARLY);

        assertThat(result).isEqualTo(Instant.parse("2027-03-15T00:00:00Z"));
    }

    @Test
    void nextOccurrence_nullInterval_onlyTruncatesToMidnight() {
        Instant date = Instant.parse("2026-03-15T13:45:30Z");

        Instant result = DateUtils.nextOccurrence(date, null);

        assertThat(result).isEqualTo(Instant.parse("2026-03-15T00:00:00Z"));
    }

    @Test
    void startOfMonth_returnsFirstDayAtMidnight() {
        ZonedDateTime moment = ZonedDateTime.of(2026, 2, 15, 10, 0, 0, 0, ZoneOffset.UTC);

        Instant result = DateUtils.startOfMonth(moment);

        assertThat(result).isEqualTo(Instant.parse("2026-02-01T00:00:00Z"));
    }

    @Test
    void endOfMonth_nonLeapFebruary_returnsDay28JustBeforeMidnight() {
        ZonedDateTime moment = ZonedDateTime.of(2026, 2, 15, 10, 0, 0, 0, ZoneOffset.UTC);

        Instant result = DateUtils.endOfMonth(moment);

        assertThat(result).isEqualTo(Instant.parse("2026-02-28T23:59:59.999999Z"));
    }

    @Test
    void endOfMonth_leapFebruary_returnsDay29() {
        ZonedDateTime moment = ZonedDateTime.of(2028, 2, 10, 10, 0, 0, 0, ZoneOffset.UTC);

        Instant result = DateUtils.endOfMonth(moment);

        assertThat(result).isEqualTo(Instant.parse("2028-02-29T23:59:59.999999Z"));
    }

    @Test
    void formatPeriodLabel_formatsMonthDayRangeAndYear() {
        Instant from = Instant.parse("2026-05-01T00:00:00Z");
        Instant to = Instant.parse("2026-05-31T23:59:59Z");

        String label = DateUtils.formatPeriodLabel(from, to);

        assertThat(label).isEqualTo("May 1 - 31, 2026");
    }

    @Test
    void periodKey_formatsYearAndMonth() {
        ZonedDateTime moment = ZonedDateTime.of(2026, 5, 15, 0, 0, 0, 0, ZoneOffset.UTC);

        assertThat(DateUtils.periodKey(moment)).isEqualTo("2026-05");
    }

    @Test
    void periodLabel_formatsMonthNameAndYear() {
        ZonedDateTime moment = ZonedDateTime.of(2026, 5, 15, 0, 0, 0, 0, ZoneOffset.UTC);

        assertThat(DateUtils.periodLabel(moment)).isEqualTo("May 2026");
    }

    @Test
    void percentageChange_previousZeroAndCurrentPositive_returns100() {
        assertThat(DateUtils.percentageChange(0, 50)).isEqualTo(100.0);
    }

    @Test
    void percentageChange_previousZeroAndCurrentZero_returns0() {
        assertThat(DateUtils.percentageChange(0, 0)).isEqualTo(0.0);
    }

    @Test
    void percentageChange_increaseFromNonZeroPrevious_computesPositivePercentage() {
        assertThat(DateUtils.percentageChange(100, 150)).isEqualTo(50.0);
    }

    @Test
    void percentageChange_decreaseFromNonZeroPrevious_computesNegativePercentage() {
        assertThat(DateUtils.percentageChange(100, 50)).isEqualTo(-50.0);
    }

    @Test
    void zoneOrDefault_validTimezone_returnsThatZone() {
        assertThat(DateUtils.zoneOrDefault("America/New_York", "UTC").getId())
                .isEqualTo("America/New_York");
    }

    @Test
    void zoneOrDefault_invalidTimezone_returnsFallback() {
        assertThat(DateUtils.zoneOrDefault("Not/AZone", "UTC").getId()).isEqualTo("UTC");
    }
}
