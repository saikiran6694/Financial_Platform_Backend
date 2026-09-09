package com.arthium.finance.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyOtpRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(max = 6) String otp
) {
}
