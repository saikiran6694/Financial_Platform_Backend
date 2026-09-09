package com.arthium.finance.cron;

import com.arthium.finance.config.AppProperties;
import com.arthium.finance.report.ReportSchedule;
import com.arthium.finance.report.ReportScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(2)
public class JobLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(JobLoader.class);
    private static final String BUDGET_CRON = "0 9 * * *";

    private final ReportScheduleRepository scheduleRepository;
    private final DynamicJobScheduler jobScheduler;
    private final AppProperties properties;

    public JobLoader(ReportScheduleRepository scheduleRepository,
                     DynamicJobScheduler jobScheduler,
                     AppProperties properties) {
        this.scheduleRepository = scheduleRepository;
        this.jobScheduler = jobScheduler;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Loading scheduled jobs from the database...");

        List<ReportSchedule> schedules = scheduleRepository.findAll();

        for (ReportSchedule schedule : schedules) {
            if (schedule.getUserId() == null || schedule.getScheduledTime() == null) {
                continue;
            }

            String userId = schedule.getUserId().toHexString();
            String cronExpression = schedule.getScheduledTime();
            String timezone = schedule.getTimezone() != null && !schedule.getTimezone().isBlank()
                    ? schedule.getTimezone()
                    : properties.getDefaultTimezone();

            log.info("Found schedule | user_id={} | cron={} | tz={}", userId, cronExpression, timezone);

            jobScheduler.schedule("report", cronExpression, timezone, userId);
            jobScheduler.schedule("transaction", cronExpression, timezone, userId);
            jobScheduler.schedule("budget", BUDGET_CRON, timezone, userId);
        }

        log.info("Scheduled {} job(s) for {} user(s)", jobScheduler.scheduledCount(), schedules.size());
    }
}
