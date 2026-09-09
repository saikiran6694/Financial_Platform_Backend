package com.arthium.finance.cron;

import com.arthium.finance.common.CronUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;


@Component
public class DynamicJobScheduler {

    private static final Logger log = LoggerFactory.getLogger(DynamicJobScheduler.class);

    private final TaskScheduler taskScheduler;
    private final ObjectProvider<List<UserJob>> jobsProvider;
    private final Map<String, ScheduledFuture<?>> scheduledJobs = new ConcurrentHashMap<>();

    private volatile Map<String, UserJob> jobsByName;

    public DynamicJobScheduler(TaskScheduler taskScheduler, ObjectProvider<List<UserJob>> jobsProvider) {
        this.taskScheduler = taskScheduler;
        this.jobsProvider = jobsProvider;
    }

    public void schedule(String name, String cronExpression, String timezone, String userId) {
        UserJob job = resolveJob(name);
        if (job == null) {
            log.warn("No job registered under the name '{}'", name);
            return;
        }

        String jobId = jobId(name, userId);

        try {
            CronTrigger trigger = new CronTrigger(CronUtils.toSpringCron(cronExpression), ZoneId.of(timezone));
            cancel(jobId);

            ScheduledFuture<?> future = taskScheduler.schedule(() -> runSafely(job, userId), trigger);
            if (future != null) {
                scheduledJobs.put(jobId, future);
            }

            log.info("Scheduled {} at {} ({})", jobId, cronExpression, timezone);
        } catch (Exception e) {
            log.error("Failed to schedule {} with cron '{}' in {}: {}", jobId, cronExpression, timezone, e.getMessage());
        }
    }

    public void reschedule(String name, String cronExpression, String timezone, String userId) {
        remove(name, userId);
        schedule(name, cronExpression, timezone, userId);
    }

    public void remove(String name, String userId) {
        String jobId = jobId(name, userId);
        if (cancel(jobId)) {
            log.info("Removed job {}", jobId);
        }
    }

    public int scheduledCount() {
        return scheduledJobs.size();
    }

    private boolean cancel(String jobId) {
        ScheduledFuture<?> existing = scheduledJobs.remove(jobId);
        if (existing != null) {
            existing.cancel(false);
            return true;
        }
        return false;
    }

    private void runSafely(UserJob job, String userId) {
        try {
            job.run(userId);
        } catch (Exception e) {
            log.error("Job {} failed for user {}", job.name(), userId, e);
        }
    }

    private UserJob resolveJob(String name) {
        Map<String, UserJob> snapshot = jobsByName;
        if (snapshot == null) {
            synchronized (this) {
                if (jobsByName == null) {
                    Map<String, UserJob> resolved = new HashMap<>();
                    for (UserJob job : jobsProvider.getIfAvailable(List::of)) {
                        resolved.put(job.name(), job);
                    }
                    jobsByName = resolved;
                }
                snapshot = jobsByName;
            }
        }
        return snapshot.get(name);
    }

    private static String jobId(String name, String userId) {
        return name + "_" + userId;
    }
}
