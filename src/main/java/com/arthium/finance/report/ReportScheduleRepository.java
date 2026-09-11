package com.arthium.finance.report;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReportScheduleRepository extends JpaRepository<ReportSchedule, UUID> {

    Optional<ReportSchedule> findByUserId(UUID userId);
}
