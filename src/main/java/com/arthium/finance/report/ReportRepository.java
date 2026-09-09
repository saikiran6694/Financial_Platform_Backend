package com.arthium.finance.report;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ReportRepository extends MongoRepository<Report, ObjectId> {

    Page<Report> findByUserId(ObjectId userId, Pageable pageable);
}
