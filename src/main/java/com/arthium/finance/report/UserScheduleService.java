package com.arthium.finance.report;

import com.arthium.finance.config.AppProperties;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Reads a user's configured cron timezone from the scheduler collection.
 * Extracted into its own bean so budgets, the chat layer and the cron jobs can
 * share it without depending on each other.
 */
@Service
public class UserScheduleService {

    private final ReportScheduleRepository scheduleRepository;
    private final AppProperties properties;

    public UserScheduleService(ReportScheduleRepository scheduleRepository, AppProperties properties) {
        this.scheduleRepository = scheduleRepository;
        this.properties = properties;
    }

    /**
     * Port of services/budgets.py::get_user_timezone.
     *
     * The "current calendar month" used for budget spend and alerting has to be
     * evaluated in the user's own timezone (the same one their daily 09:00
     * budget check runs in), not UTC, or the month boundary can disagree with
     * their local calendar around month-end.
     */
    public String getUserTimezone(String userId) {
        return findByUserId(userId)
                .map(ReportSchedule::getTimezone)
                .filter(tz -> tz != null && !tz.isBlank())
                .orElse(properties.getDefaultTimezone());
    }

    public Optional<ReportSchedule> findByUserId(String userId) {
        if (!ObjectId.isValid(userId)) {
            return Optional.empty();
        }
        return scheduleRepository.findByUserId(new ObjectId(userId));
    }
}
