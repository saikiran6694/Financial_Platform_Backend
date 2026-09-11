package com.arthium.finance.cron;

import com.arthium.finance.common.DateUtils;
import com.arthium.finance.transaction.Transaction;
import com.arthium.finance.transaction.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class RecurringTransactionJob implements UserJob {

    private static final Logger log = LoggerFactory.getLogger(RecurringTransactionJob.class);

    private final TransactionRepository transactionRepository;

    public RecurringTransactionJob(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public String name() {
        return "transaction";
    }

    @Override
    public void run(String userId) {
        Instant now = Instant.now();
        int processed = 0;
        int failed = 0;

        log.info("Running recurring transactions for user: {}", userId);

        try {
            List<Transaction> due = transactionRepository
                    .findByUserIdAndRecurringTrueAndNextRecurringDateLessThanEqual(UUID.fromString(userId), now);

            if (due.isEmpty()) {
                log.info("No recurring transactions for user {}", userId);
                return;
            }

            log.info("Processing {} recurring transactions", due.size());

            List<Transaction> inserts = new ArrayList<>();
            List<Transaction> updates = new ArrayList<>();

            for (Transaction source : due) {
                try {
                    Instant nextDate = DateUtils.nextOccurrence(
                            source.getNextRecurringDate(), source.getRecurringInterval());

                    Transaction generated = new Transaction();
                    generated.setUserId(source.getUserId());
                    generated.setTitle("Recurring - " + source.getTitle());
                    generated.setType(source.getType());
                    generated.setAmount(source.getAmount());
                    generated.setCategory(source.getCategory());
                    generated.setReceiptUrl(source.getReceiptUrl());
                    generated.setDescription(source.getDescription());
                    generated.setDate(source.getNextRecurringDate());
                    generated.setStatus(source.getStatus());
                    generated.setPaymentMethod(source.getPaymentMethod());
                    generated.setRecurring(false);
                    generated.setRecurringInterval(null);
                    generated.setNextRecurringDate(null);
                    generated.setLastProcessed(null);
                    generated.setCreatedAt(now);
                    generated.setUpdatedAt(now);
                    inserts.add(generated);

                    source.setNextRecurringDate(nextDate);
                    source.setLastProcessed(now);
                    source.setUpdatedAt(now);
                    updates.add(source);

                    processed++;
                } catch (Exception e) {
                    failed++;
                    log.error("Failed processing transaction {}", source.getId(), e);
                }
            }

            if (!inserts.isEmpty()) {
                transactionRepository.saveAll(inserts);
            }
            if (!updates.isEmpty()) {
                transactionRepository.saveAll(updates);
            }

            log.info("Processed: {} transactions, failed: {}", processed, failed);

        } catch (Exception e) {
            log.error("Error in recurring transaction job for user {}", userId, e);
        }
    }
}
