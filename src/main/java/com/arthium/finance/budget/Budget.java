package com.arthium.finance.budget;

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
@Table(name = "budgets")
public class Budget {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "category")
    private String category;

    @Column(name = "limit_amount")
    private long limitAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "period")
    private BudgetPeriod period = BudgetPeriod.MONTHLY;

    @Column(name = "alert_threshold")
    private double alertThreshold = 0.8;

    @Column(name = "is_active")
    private boolean active = true;

    /**
     * Tracks the last threshold level already emailed about, per period
     * ("YYYY-MM" key plus "warning" | "exceeded"), so the daily job does not
     * re-send the same alert every morning.
     */
    @Column(name = "last_alerted_period")
    private String lastAlertedPeriod;

    @Column(name = "last_alerted_level")
    private String lastAlertedLevel;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public long getLimitAmount() { return limitAmount; }
    public void setLimitAmount(long limitAmount) { this.limitAmount = limitAmount; }

    public BudgetPeriod getPeriod() { return period; }
    public void setPeriod(BudgetPeriod period) { this.period = period; }

    public double getAlertThreshold() { return alertThreshold; }
    public void setAlertThreshold(double alertThreshold) { this.alertThreshold = alertThreshold; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getLastAlertedPeriod() { return lastAlertedPeriod; }
    public void setLastAlertedPeriod(String lastAlertedPeriod) { this.lastAlertedPeriod = lastAlertedPeriod; }

    public String getLastAlertedLevel() { return lastAlertedLevel; }
    public void setLastAlertedLevel(String lastAlertedLevel) { this.lastAlertedLevel = lastAlertedLevel; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
