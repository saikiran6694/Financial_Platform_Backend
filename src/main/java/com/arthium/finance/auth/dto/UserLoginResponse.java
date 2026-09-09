package com.arthium.finance.auth.dto;

import com.arthium.finance.report.dto.ReportSettingLoginResponse;
import com.arthium.finance.user.dto.UserPrivateDto;

/** Port of schemas/user_schema.py::UserLoginResponse. Expiries are in milliseconds. */
public record UserLoginResponse(
        UserPrivateDto user,
        String accessToken,
        long expiresIn,
        String refreshToken,
        long refreshTokenExpiresIn,
        ReportSettingLoginResponse reportSettings
) {
}
