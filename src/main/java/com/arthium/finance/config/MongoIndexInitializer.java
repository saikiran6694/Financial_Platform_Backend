package com.arthium.finance.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Port of bootstrap/db.py::_ensure_indexes.
 *
 * Every hot-path query filters by user_id (plus a secondary field like date or
 * category); without these, they are full collection scans. Index creation is
 * idempotent, so running it on every boot is safe.
 */
@Component
@Order(1)
public class MongoIndexInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MongoIndexInitializer.class);

    private final MongoTemplate mongoTemplate;

    public MongoIndexInitializer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        mongoTemplate.getDb().runCommand(new org.bson.Document("ping", 1));
        log.info("Connected to MongoDB: {}", mongoTemplate.getDb().getName());

        mongoTemplate.indexOps("users")
                .createIndex(new Index().on("email", Sort.Direction.ASC).unique());

        mongoTemplate.indexOps("transactions")
                .createIndex(new Index().on("user_id", Sort.Direction.ASC).on("date", Sort.Direction.ASC));
        mongoTemplate.indexOps("transactions")
                .createIndex(new Index().on("user_id", Sort.Direction.ASC).on("category", Sort.Direction.ASC));

        mongoTemplate.indexOps("budgets")
                .createIndex(new Index().on("user_id", Sort.Direction.ASC).on("category", Sort.Direction.ASC));

        mongoTemplate.indexOps("reports")
                .createIndex(new Index().on("user_id", Sort.Direction.ASC).on("created_at", Sort.Direction.ASC));

        mongoTemplate.indexOps("report_settings")
                .createIndex(new Index().on("user_id", Sort.Direction.ASC).unique());
        mongoTemplate.indexOps("report_settings")
                .createIndex(new Index().on("next_report_date", Sort.Direction.ASC));

        mongoTemplate.indexOps("scheduler")
                .createIndex(new Index().on("user_id", Sort.Direction.ASC).unique());

        mongoTemplate.indexOps("forgot_password")
                .createIndex(new Index().on("email", Sort.Direction.ASC).on("otp", Sort.Direction.ASC));
        // TTL index: Mongo auto-deletes OTP docs once expires_at is in the past.
        mongoTemplate.indexOps("forgot_password")
                .createIndex(new Index().on("expires_at", Sort.Direction.ASC).expire(Duration.ZERO));

        log.info("MongoDB indexes ensured");
    }
}
