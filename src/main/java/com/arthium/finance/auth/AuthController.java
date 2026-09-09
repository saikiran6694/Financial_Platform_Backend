package com.arthium.finance.auth;

import com.arthium.finance.auth.dto.ForgotPasswordRequest;
import com.arthium.finance.auth.dto.ForgotPasswordResponse;
import com.arthium.finance.auth.dto.RefreshTokenRequest;
import com.arthium.finance.auth.dto.RefreshTokenResponse;
import com.arthium.finance.auth.dto.ResetPasswordRequest;
import com.arthium.finance.auth.dto.ResetPasswordResponse;
import com.arthium.finance.auth.dto.UserCreateRequest;
import com.arthium.finance.auth.dto.UserLoginRequest;
import com.arthium.finance.auth.dto.UserLoginResponse;
import com.arthium.finance.auth.dto.VerifyOtpRequest;
import com.arthium.finance.auth.dto.VerifyOtpResponse;
import com.arthium.finance.user.dto.UserPrivateDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserPrivateDto register(@Valid @RequestBody UserCreateRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public UserLoginResponse login(@Valid @RequestBody UserLoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh-token")
    public RefreshTokenResponse refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refreshToken(request.refreshToken());
    }

    @PostMapping("/forgot-password/send-otp")
    public ForgotPasswordResponse sendOtp(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.sendForgotPasswordOtp(request.email());
        return new ForgotPasswordResponse("OTP sent to email if it exists in our system");
    }

    @PostMapping("/forgot-password/verify-otp")
    public VerifyOtpResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        boolean valid = authService.verifyForgotPasswordOtp(request.email(), request.otp());
        return new VerifyOtpResponse("OTP verification completed", valid);
    }

    @PostMapping("/forgot-password/reset")
    public ResetPasswordResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.email(), request.newPassword());
        return new ResetPasswordResponse("Password reset successfully");
    }
}
