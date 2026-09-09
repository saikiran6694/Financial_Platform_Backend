package com.arthium.finance.auth;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.Optional;

public interface ForgotPasswordOtpRepository extends MongoRepository<ForgotPasswordOtp, ObjectId> {

    Optional<ForgotPasswordOtp> findFirstByEmailAndExpiresAtGreaterThan(String email, Instant moment);

    Optional<ForgotPasswordOtp> findFirstByEmailAndOtpAndExpiresAtGreaterThanAndVerified(
            String email, String otp, Instant moment, boolean verified);

    Optional<ForgotPasswordOtp> findFirstByEmailAndExpiresAtGreaterThanAndVerified(
            String email, Instant moment, boolean verified);
}
