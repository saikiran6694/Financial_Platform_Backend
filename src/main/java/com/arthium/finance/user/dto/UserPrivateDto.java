package com.arthium.finance.user.dto;

import com.arthium.finance.user.User;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Port of schemas/user_schema.py::UserPrivate.
 * FastAPI serialised response models by alias, so the id field goes out as
 * "_id" — kept identical here so existing clients don't break.
 */
public record UserPrivateDto(
        @JsonProperty("_id") String id,
        String name,
        String profilePicture,
        String email,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserPrivateDto from(User user) {
        return new UserPrivateDto(
                user.getIdAsString(),
                user.getName(),
                user.getProfilePicture(),
                user.getEmail(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
