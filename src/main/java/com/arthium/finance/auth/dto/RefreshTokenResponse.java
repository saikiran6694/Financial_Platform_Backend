package com.arthium.finance.auth.dto;

public record RefreshTokenResponse(String accessToken, long expiresIn) {
}
