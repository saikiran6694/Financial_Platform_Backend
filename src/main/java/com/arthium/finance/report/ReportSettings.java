package com.arthium.finance.report;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "report_settings")
public class ReportSettings {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency")
    private ReportFrequency frequency = ReportFrequency.MONTHLY;

    @Column(name = "is_enabled")
    private boolean enabled;

    @Column(name = "next_report_date")
    private Instant nextReportDate;

    @Column(name = "last_sent_date")
    private Instant lastSentDate;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public ReportFrequency getFrequency() { return frequency; }
    public void setFrequency(ReportFrequency frequency) { this.frequency = frequency; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Instant getNextReportDate() { return nextReportDate; }
    public void setNextReportDate(Instant nextReportDate) { this.nextReportDate = nextReportDate; }

    public Instant getLastSentDate() { return lastSentDate; }
    public void setLastSentDate(Instant lastSentDate) { this.lastSentDate = lastSentDate; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
