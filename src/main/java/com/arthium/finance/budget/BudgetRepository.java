package com.arthium.finance.budget;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends MongoRepository<Budget, ObjectId> {

    List<Budget> findByUserIdOrderByCreatedAtDesc(ObjectId userId);

    List<Budget> findByUserIdAndActiveTrue(ObjectId userId);

    Optional<Budget> findByIdAndUserId(ObjectId id, ObjectId userId);
}
