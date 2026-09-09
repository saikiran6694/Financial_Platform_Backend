package com.arthium.finance.transaction;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TransactionRepository extends MongoRepository<Transaction, ObjectId> {

    Optional<Transaction> findByIdAndUserId(ObjectId id, ObjectId userId);
}
