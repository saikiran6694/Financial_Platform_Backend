package com.arthium.finance.auth.dto;

import com.arthium.finance.report.dto.ReportSettingLoginResponse;
import com.arthium.finance.user.dto.UserPrivateDto;

public record UserLoginResponse(
        UserPrivateDto user,
        String accessToken,
        long expiresIn,
        String refreshToken,
        long refreshTokenExpiresIn,
        ReportSettingLoginResponse reportSettings
) {
}
