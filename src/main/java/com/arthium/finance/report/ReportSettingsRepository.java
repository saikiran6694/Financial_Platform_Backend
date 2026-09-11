package com.arthium.finance.report;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ReportSettingsRepository extends JpaRepository<ReportSettings, UUID> {

    Optional<ReportSettings> findByUserId(UUID userId);

    Optional<ReportSettings> findByUserIdAndEnabledTrueAndNextReportDateLessThanEqual(UUID userId, Instant moment);
}
