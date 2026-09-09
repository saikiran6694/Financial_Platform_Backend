package com.arthium.finance.analytics;

import com.arthium.finance.common.DateUtils;
import com.arthium.finance.report.DateRange;

import java.time.Instant;
import java.time.ZonedDateTime;

public final class DateRangeResolver {

    private DateRangeResolver() {
    }

    public record Resolved(Instant from, Instant to, DateRange value, String label) {
    }

    public static Resolved resolve(DateRange preset, Instant customFrom, Instant customTo) {
        ZonedDateTime now = ZonedDateTime.now(DateUtils.UTC);

        if (preset == null) {
            return allTime();
        }

        return switch (preset) {
            case LAST_30_DAYS -> new Resolved(
                    now.minusDays(30).toInstant(), now.toInstant(), preset, "Last 30 Days");

            case LAST_MONTH -> new Resolved(
                    now.withDayOfMonth(1).minusMonths(1).toInstant(),
                    now.withDayOfMonth(1).minusDays(1).toInstant(),
                    preset, "Last Month");

            case LAST_3_MONTHS -> new Resolved(
                    now.minusMonths(3).toInstant(), now.toInstant(), preset, "Last 3 Months");

            case LAST_YEAR -> new Resolved(
                    now.minusYears(1).toInstant(), now.toInstant(), preset, "Last Year");

            case THIS_MONTH -> new Resolved(
                    now.withDayOfMonth(1).toInstant(), now.toInstant(), preset, "This Month");

            case THIS_YEAR -> new Resolved(
                    now.withMonth(1).withDayOfMonth(1).toInstant(), now.toInstant(), preset, "This Year");

            case CUSTOM -> (customFrom != null && customTo != null)
                    ? new Resolved(customFrom, customTo, preset, "Custom Range")
                    : allTime();

            case ALL_TIME -> allTime();
        };
    }

    private static Resolved allTime() {
        return new Resolved(null, null, DateRange.ALL_TIME, "All Time");
    }
}
