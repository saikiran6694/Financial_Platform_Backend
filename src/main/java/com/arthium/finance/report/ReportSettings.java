package com.arthium.finance.report;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Document(collection = "report_settings")
public class ReportSettings {

    @Id
    private ObjectId id;

    @Field("user_id")
    private ObjectId userId;

    @Field("frequency")
    private ReportFrequency frequency = ReportFrequency.MONTHLY;

    @Field("is_enabled")
    private boolean enabled;

    @Field("next_report_date")
    private Instant nextReportDate;

    @Field("last_sent_date")
    private Instant lastSentDate;

    @Field("created_at")
    private Instant createdAt;

    @Field("updated_at")
    private Instant updatedAt;

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }

    public ObjectId getUserId() { return userId; }
    public void setUserId(ObjectId userId) { this.userId = userId; }

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
