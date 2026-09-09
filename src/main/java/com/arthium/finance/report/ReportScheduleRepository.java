package com.arthium.finance.report;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ReportScheduleRepository extends MongoRepository<ReportSchedule, ObjectId> {

    Optional<ReportSchedule> findByUserId(ObjectId userId);
}
