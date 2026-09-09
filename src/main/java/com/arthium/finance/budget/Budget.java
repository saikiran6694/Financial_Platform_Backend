package com.arthium.finance.budget;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Document(collection = "budgets")
public class Budget {

    @Id
    private ObjectId id;

    @Field("user_id")
    private ObjectId userId;

    @Field("category")
    private String category;

    @Field("limit_amount")
    private long limitAmount;

    @Field("period")
    private BudgetPeriod period = BudgetPeriod.MONTHLY;

    @Field("alert_threshold")
    private double alertThreshold = 0.8;

    @Field("is_active")
    private boolean active = true;

    /**
     * Tracks the last threshold level already emailed about, per period
     * ("YYYY-MM" key plus "warning" | "exceeded"), so the daily job does not
     * re-send the same alert every morning.
     */
    @Field("last_alerted_period")
    private String lastAlertedPeriod;

    @Field("last_alerted_level")
    private String lastAlertedLevel;

    @Field("created_at")
    private Instant createdAt;

    @Field("updated_at")
    private Instant updatedAt;

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }

    public ObjectId getUserId() { return userId; }
    public void setUserId(ObjectId userId) { this.userId = userId; }

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
