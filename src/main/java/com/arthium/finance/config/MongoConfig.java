package com.arthium.finance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;

/**
 * Enables @Transactional for MongoDB, which the registration flow relies on
 * (the Python version used an explicit session + transaction there).
 * Requires a replica set or a sharded cluster — MongoDB Atlas qualifies.
 */
@Configuration
public class MongoConfig {

    @Bean
    public MongoTransactionManager mongoTransactionManager(MongoDatabaseFactory databaseFactory) {
        return new MongoTransactionManager(databaseFactory);
    }
}
