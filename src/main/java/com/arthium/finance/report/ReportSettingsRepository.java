package com.arthium.finance.report;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ReportSettingsRepository extends MongoRepository<ReportSettings, ObjectId> {

    Optional<ReportSettings> findByUserId(ObjectId userId);
}
