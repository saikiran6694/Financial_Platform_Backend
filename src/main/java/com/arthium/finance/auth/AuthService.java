package com.arthium.finance.auth;

import com.arthium.finance.auth.dto.RefreshTokenResponse;
import com.arthium.finance.auth.dto.UserCreateRequest;
import com.arthium.finance.auth.dto.UserLoginRequest;
import com.arthium.finance.auth.dto.UserLoginResponse;
import com.arthium.finance.common.ApiException;
import com.arthium.finance.common.DateUtils;
import com.arthium.finance.mail.ForgotPasswordMailer;
import com.arthium.finance.report.ReportFrequency;
import com.arthium.finance.report.ReportSettings;
import com.arthium.finance.report.ReportSettingsRepository;
import com.arthium.finance.report.dto.ReportSettingLoginResponse;
import com.arthium.finance.security.JwtService;
import com.arthium.finance.user.User;
import com.arthium.finance.user.UserRepository;
import com.arthium.finance.user.dto.UserPrivateDto;
import org.bson.types.ObjectId;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/** Port of services/auth.py. */
@Service
public class AuthService {

    private static final Duration OTP_TTL = Duration.ofMinutes(10);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final ReportSettingsRepository reportSettingsRepository;
    private final ForgotPasswordOtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ForgotPasswordMailer forgotPasswordMailer;

    public AuthService(UserRepository userRepository,
                       ReportSettingsRepository reportSettingsRepository,
                       ForgotPasswordOtpRepository otpRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       ForgotPasswordMailer forgotPasswordMailer) {
        this.userRepository = userRepository;
        this.reportSettingsRepository = reportSettingsRepository;
        this.otpRepository = otpRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.forgotPasswordMailer = forgotPasswordMailer;
    }

    // ── Registration ─────────────────────────────────────────────────────────

    /**
     * Creates the user and their default report settings in one transaction,
     * mirroring the session/transaction the Python service used.
     */
    @Transactional
    public UserPrivateDto register(UserCreateRequest request) {
        String email = normalise(request.email());

        if (userRepository.existsByEmail(email)) {
            throw ApiException.badRequest("email already exists");
        }

        Instant now = Instant.now();

        User user = new User();
        user.setName(request.name());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setProfilePicture(null);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        User created = userRepository.save(user);

        ReportSettings settings = new ReportSettings();
        settings.setUserId(created.getId());
        settings.setFrequency(ReportFrequency.MONTHLY);
        settings.setEnabled(true);
        settings.setLastSentDate(null);
        settings.setNextReportDate(DateUtils.nextReportDate(null));
        settings.setCreatedAt(now);
        settings.setUpdatedAt(now);

        reportSettingsRepository.save(settings);

        return UserPrivateDto.from(created);
    }

    // ── Login / refresh ──────────────────────────────────────────────────────

    public UserLoginResponse login(UserLoginRequest request) {
        User user = userRepository.findByEmail(normalise(request.email()))
                .orElseThrow(() -> ApiException.unauthorized("Incorrect email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw ApiException.unauthorized("Incorrect email or password");
        }

        String userId = user.getIdAsString();
        String accessToken = jwtService.createAccessToken(userId);
        String refreshToken = jwtService.createRefreshToken(userId);

        ReportSettings settings = reportSettingsRepository.findByUserId(user.getId()).orElse(null);

        return new UserLoginResponse(
                UserPrivateDto.from(user),
                accessToken,
                jwtService.getAccessTokenTtl().toMillis(),
                refreshToken,
                jwtService.getRefreshTokenTtl().toMillis(),
                ReportSettingLoginResponse.from(settings)
        );
    }

    public RefreshTokenResponse refreshToken(String refreshToken) {
        String userId = jwtService.verifyToken(refreshToken)
                .orElseThrow(() -> ApiException.unauthorized("Invalid or expired refresh token"));

        if (!ObjectId.isValid(userId) || userRepository.findById(new ObjectId(userId)).isEmpty()) {
            throw ApiException.unauthorized("User not found");
        }

        return new RefreshTokenResponse(
                jwtService.createAccessToken(userId),
                jwtService.getAccessTokenTtl().toMillis()
        );
    }

    // ── Forgot password ──────────────────────────────────────────────────────

    public void sendForgotPasswordOtp(String rawEmail) {
        String email = normalise(rawEmail);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("User not found"));

        Optional<ForgotPasswordOtp> existing =
                otpRepository.findFirstByEmailAndExpiresAtGreaterThan(email, Instant.now());

        if (existing.isPresent()) {
            throw ApiException.badRequest(
                    "An OTP has already been sent to this email. Please check your email or try again later.");
        }

        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        Instant now = Instant.now();

        ForgotPasswordOtp record = new ForgotPasswordOtp();
        record.setEmail(email);
        record.setOtp(otp);
        record.setExpiresAt(now.plus(OTP_TTL));
        record.setVerified(false);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);

        ForgotPasswordOtp saved = otpRepository.save(record);
        if (saved.getId() == null) {
            throw ApiException.internal("Failed to generate OTP. Please try again.");
        }

        forgotPasswordMailer.send(email, user.getName(), otp);
    }

    public boolean verifyForgotPasswordOtp(String rawEmail, String otp) {
        String email = normalise(rawEmail);

        ForgotPasswordOtp record = otpRepository
                .findFirstByEmailAndOtpAndExpiresAtGreaterThanAndVerified(email, otp, Instant.now(), false)
                .orElseThrow(() -> ApiException.badRequest("Invalid or expired OTP"));

        record.setVerified(true);
        record.setUpdatedAt(Instant.now());
        otpRepository.save(record);

        return true;
    }

    public void resetPassword(String rawEmail, String newPassword) {
        String email = normalise(rawEmail);

        ForgotPasswordOtp record = otpRepository
                .findFirstByEmailAndExpiresAtGreaterThanAndVerified(email, Instant.now(), true)
                .orElseThrow(() -> ApiException.badRequest("OTP verification required before resetting password"));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.internal("Failed to reset password. Please try again."));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        // Invalidate the OTP so it cannot be reused for another reset.
        record.setVerified(false);
        record.setUpdatedAt(Instant.now());
        otpRepository.save(record);
    }

    private static String normalise(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ENGLISH);
    }
}
