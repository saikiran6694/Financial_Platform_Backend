package com.arthium.finance.transaction;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Document(collection = "transactions")
public class Transaction {

    @Id
    private ObjectId id;

    @Field("user_id")
    private ObjectId userId;

    @Field("title")
    private String title;

    @Field("type")
    private TransactionType type;

    @Field("amount")
    private long amount;

    @Field("category")
    private String category;

    @Field("receipt_url")
    private String receiptUrl;

    @Field("recurring_interval")
    private RecurringInterval recurringInterval;

    @Field("next_recurring_date")
    private Instant nextRecurringDate;

    @Field("last_processed")
    private Instant lastProcessed;

    @Field("is_recurring")
    private boolean recurring;

    @Field("description")
    private String description;

    @Field("date")
    private Instant date;

    @Field("status")
    private TransactionStatus status = TransactionStatus.COMPLETED;

    @Field("payment_method")
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    @Field("created_at")
    private Instant createdAt;

    @Field("updated_at")
    private Instant updatedAt;

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }

    public ObjectId getUserId() { return userId; }
    public void setUserId(ObjectId userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getReceiptUrl() { return receiptUrl; }
    public void setReceiptUrl(String receiptUrl) { this.receiptUrl = receiptUrl; }

    public RecurringInterval getRecurringInterval() { return recurringInterval; }
    public void setRecurringInterval(RecurringInterval recurringInterval) { this.recurringInterval = recurringInterval; }

    public Instant getNextRecurringDate() { return nextRecurringDate; }
    public void setNextRecurringDate(Instant nextRecurringDate) { this.nextRecurringDate = nextRecurringDate; }

    public Instant getLastProcessed() { return lastProcessed; }
    public void setLastProcessed(Instant lastProcessed) { this.lastProcessed = lastProcessed; }

    public boolean isRecurring() { return recurring; }
    public void setRecurring(boolean recurring) { this.recurring = recurring; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getDate() { return date; }
    public void setDate(Instant date) { this.date = date; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
