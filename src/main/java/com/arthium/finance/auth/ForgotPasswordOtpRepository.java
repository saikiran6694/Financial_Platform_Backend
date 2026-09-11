package com.arthium.finance.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ForgotPasswordOtpRepository extends JpaRepository<ForgotPasswordOtp, UUID> {

    Optional<ForgotPasswordOtp> findFirstByEmailAndExpiresAtGreaterThan(String email, Instant moment);

    Optional<ForgotPasswordOtp> findFirstByEmailAndOtpAndExpiresAtGreaterThanAndVerified(
            String email, String otp, Instant moment, boolean verified);

    Optional<ForgotPasswordOtp> findFirstByEmailAndExpiresAtGreaterThanAndVerified(
            String email, Instant moment, boolean verified);

    @Modifying
    @Query("DELETE FROM ForgotPasswordOtp o WHERE o.expiresAt < :moment")
    int deleteByExpiresAtBefore(Instant moment);
}
