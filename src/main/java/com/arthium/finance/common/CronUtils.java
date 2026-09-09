package com.arthium.finance.common;

/**
 * The scheduler collection stores standard 5-field Unix cron expressions
 * (the format APScheduler's CronTrigger.from_crontab expected).
 * Spring's CronTrigger uses 6 fields, with seconds first — so we prepend "0".
 */
public final class CronUtils {

    private CronUtils() {
    }

    public static String toSpringCron(String unixCron) {
        if (unixCron == null || unixCron.isBlank()) {
            throw ApiException.badRequest("Cron expression must not be empty");
        }
        String trimmed = unixCron.trim().replaceAll("\\s+", " ");
        String[] parts = trimmed.split(" ");
        if (parts.length == 6) {
            return trimmed;
        }
        if (parts.length != 5) {
            throw ApiException.badRequest("Invalid cron expression: " + unixCron);
        }
        return "0 " + trimmed;
    }
}
