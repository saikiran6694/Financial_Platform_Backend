package com.arthium.finance.cron;

import com.arthium.finance.auth.ForgotPasswordOtpRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Postgres has no equivalent of Mongo's TTL index, which used to expire
 * {@code forgot_password} rows automatically. Expired OTPs are already
 * invisible to lookups (they all filter on {@code expires_at > now()}), so
 * this just keeps the table from growing unbounded.
 */
@Component
public class ForgotPasswordCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(ForgotPasswordCleanupJob.class);

    private final ForgotPasswordOtpRepository otpRepository;

    public ForgotPasswordCleanupJob(ForgotPasswordOtpRepository otpRepository) {
        this.otpRepository = otpRepository;
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void purgeExpired() {
        int deleted = otpRepository.deleteByExpiresAtBefore(Instant.now());
        if (deleted > 0) {
            log.info("Purged {} expired forgot-password OTP row(s)", deleted);
        }
    }
}
