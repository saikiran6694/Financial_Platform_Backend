package com.arthium.finance.common;

import com.arthium.finance.transaction.RecurringInterval;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Port of utils/utils.py and utils/date.py date helpers.
 */
public final class DateUtils {

    public static final ZoneId UTC = ZoneOffset.UTC;

    private DateUtils() {
    }

    /** calculate_next_report_date: +1 month, snapped to the first of that month at midnight UTC. */
    public static Instant nextReportDate(Instant lastSentDate) {
        Instant base = lastSentDate != null ? lastSentDate : Instant.now();
        return base.atZone(UTC)
                .plusMonths(1)
                .withDayOfMonth(1)
                .toLocalDate()
                .atStartOfDay(UTC)
                .toInstant();
    }

    /** calculate_next_occurrence: truncate to midnight, then advance by the interval. */
    public static Instant nextOccurrence(Instant date, RecurringInterval interval) {
        ZonedDateTime base = date.atZone(UTC).toLocalDate().atStartOfDay(UTC);
        if (interval == null) {
            return base.toInstant();
        }
        return switch (interval) {
            case DAILY -> base.plusDays(1).toInstant();
            case WEEKLY -> base.plusWeeks(1).toInstant();
            case MONTHLY -> base.plusMonths(1).toInstant();
            case YEARLY -> base.plusYears(1).toInstant();
        };
    }

    public static Instant startOfMonth(ZonedDateTime moment) {
        return moment.toLocalDate().withDayOfMonth(1).atStartOfDay(moment.getZone()).toInstant();
    }

    public static Instant endOfMonth(ZonedDateTime moment) {
        return moment.toLocalDate()
                .withDayOfMonth(1)
                .plusMonths(1)
                .minusDays(1)
                .atTime(23, 59, 59, 999_999_000)
                .atZone(moment.getZone())
                .toInstant();
    }

    /** format_period_label: "May 1 - 31, 2026" */
    public static String formatPeriodLabel(Instant fromDate, Instant toDate) {
        ZonedDateTime from = fromDate.atZone(UTC);
        ZonedDateTime to = toDate.atZone(UTC);
        String month = from.format(DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH));
        return month + " " + from.getDayOfMonth() + " - " + to.getDayOfMonth() + ", " + to.getYear();
    }

    /** "2026-05" */
    public static String periodKey(ZonedDateTime moment) {
        return moment.format(DateTimeFormatter.ofPattern("yyyy-MM", Locale.ENGLISH));
    }

    /** "May 2026" */
    public static String periodLabel(ZonedDateTime moment) {
        return moment.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));
    }

    /** calculate_percentage_change */
    public static double percentageChange(double previous, double current) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return MoneyUtils.round(((current - previous) / Math.abs(previous)) * 100, 2);
    }

    public static ZoneId zoneOrDefault(String timezone, String fallback) {
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            return ZoneId.of(fallback);
        }
    }
}
