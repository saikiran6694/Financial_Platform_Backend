package com.arthium.finance.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Port of schemas/user_schema.py::UserCreate. */
public record UserCreateRequest(
        @NotBlank @Size(min = 1, max = 50) String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password
) {
}
