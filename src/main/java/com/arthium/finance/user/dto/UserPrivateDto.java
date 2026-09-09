package com.arthium.finance.user.dto;

import com.arthium.finance.user.User;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;


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
