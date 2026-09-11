package com.arthium.finance.cron;

import com.arthium.finance.common.DateUtils;
import com.arthium.finance.mail.ReportEmailData;
import com.arthium.finance.mail.ReportMailer;
import com.arthium.finance.report.Report;
import com.arthium.finance.report.ReportService;
import com.arthium.finance.report.ReportSettings;
import com.arthium.finance.report.ReportSettingsRepository;
import com.arthium.finance.report.ReportStatus;
import com.arthium.finance.report.dto.GeneratedReport;
import com.arthium.finance.user.User;
import com.arthium.finance.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
public class ReportJob implements UserJob {

    private static final Logger log = LoggerFactory.getLogger(ReportJob.class);

    private final ReportSettingsRepository reportSettingsRepository;
    private final UserRepository userRepository;
    private final ReportService reportService;
    private final ReportMailer reportMailer;

    public ReportJob(ReportSettingsRepository reportSettingsRepository,
                     UserRepository userRepository,
                     ReportService reportService,
                     ReportMailer reportMailer) {
        this.reportSettingsRepository = reportSettingsRepository;
        this.userRepository = userRepository;
        this.reportService = reportService;
        this.reportMailer = reportMailer;
    }

    @Override
    public String name() {
        return "report";
    }

    @Override
    public void run(String userId) {
        Instant now = Instant.now();
        ZonedDateTime lastMonth = now.atZone(DateUtils.UTC).minusMonths(1);
        Instant fromDate = DateUtils.startOfMonth(lastMonth);
        Instant toDate = DateUtils.endOfMonth(lastMonth);

        log.info("Running report job for user: {}", userId);

        try {
            Optional<ReportSettings> dueSettings = reportSettingsRepository
                    .findByUserIdAndEnabledTrueAndNextReportDateLessThanEqual(UUID.fromString(userId), now);

            if (dueSettings.isEmpty()) {
                log.info("No report due for user {}", userId);
                return;
            }
            ReportSettings settings = dueSettings.get();

            Optional<User> user = userRepository.findById(UUID.fromString(userId));
            if (user.isEmpty()) {
                log.warn("User not found: {}", userId);
                return;
            }

            GeneratedReport report = reportService.generateReport(userId, fromDate, toDate);
            boolean emailSent = false;

            if (report != null) {
                try {
                    reportMailer.send(
                            user.get().getEmail(),
                            user.get().getName(),
                            new ReportEmailData(
                                    report.periodLabel(),
                                    report.summary().income(),
                                    report.summary().expenses(),
                                    report.summary().balance(),
                                    report.summary().savingRate(),
                                    report.summary().topCategories(),
                                    report.insights()),
                            settings.getFrequency().name());
                    emailSent = true;
                } catch (Exception e) {
                    log.error("Email failed for user {}", userId, e);
                }
            }

            Report record = new Report();
            record.setUserId(UUID.fromString(userId));
            record.setSentDate(now);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);

            if (report != null && emailSent) {
                record.setPeriod(report.periodLabel());
                record.setStatus(ReportStatus.SENT);
                settings.setLastSentDate(now);
            } else {
                record.setPeriod(report != null
                        ? report.periodLabel()
                        : DateUtils.formatPeriodLabel(fromDate, toDate));
                record.setStatus(report != null ? ReportStatus.FAILED : ReportStatus.NO_ACTIVITY);
                settings.setLastSentDate(null);
            }

            settings.setNextReportDate(DateUtils.nextReportDate(now));
            settings.setUpdatedAt(now);

            reportService.saveReport(record);
            reportService.saveSettings(settings);

            log.info("Report processed for user {}", userId);

        } catch (Exception e) {
            log.error("Report job failed for user {}", userId, e);
        }
    }
}
