package com.arthium.finance.auth;

import com.arthium.finance.auth.dto.RefreshTokenResponse;
import com.arthium.finance.auth.dto.UserCreateRequest;
import com.arthium.finance.auth.dto.UserLoginRequest;
import com.arthium.finance.auth.dto.UserLoginResponse;
import com.arthium.finance.common.ApiException;
import com.arthium.finance.mail.ForgotPasswordMailer;
import com.arthium.finance.report.ReportFrequency;
import com.arthium.finance.report.ReportSettings;
import com.arthium.finance.report.ReportSettingsRepository;
import com.arthium.finance.security.JwtService;
import com.arthium.finance.user.User;
import com.arthium.finance.user.UserRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ReportSettingsRepository reportSettingsRepository;
    @Mock
    private ForgotPasswordOtpRepository otpRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private ForgotPasswordMailer forgotPasswordMailer;

    private AuthService authService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, reportSettingsRepository, otpRepository,
                passwordEncoder, jwtService, forgotPasswordMailer);
    }

    private static User userWithId(String email, String hashedPassword) {
        User user = new User();
        user.setId(new ObjectId());
        user.setEmail(email);
        user.setPassword(hashedPassword);
        user.setName("Jane Doe");
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        return user;
    }

    // ── register ─────────────────────────────────────────────────────────────

    @Test
    void register_emailAlreadyExists_throwsBadRequest() {
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);
        UserCreateRequest request = new UserCreateRequest("Jane", "existing@example.com", "password123");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_normalisesEmailBeforeCheckingAndSaving() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(new ObjectId());
            return u;
        });

        UserCreateRequest request = new UserCreateRequest("Jane", " Test@EXAMPLE.com ", "password123");
        authService.register(request);

        verify(userRepository).existsByEmail("test@example.com");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void register_success_createsDefaultMonthlyReportSettings() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(new ObjectId());
            return u;
        });

        UserCreateRequest request = new UserCreateRequest("Jane", "jane@example.com", "password123");
        var result = authService.register(request);

        assertThat(result.email()).isEqualTo("jane@example.com");
        assertThat(result.name()).isEqualTo("Jane");

        ArgumentCaptor<ReportSettings> settingsCaptor = ArgumentCaptor.forClass(ReportSettings.class);
        verify(reportSettingsRepository).save(settingsCaptor.capture());
        ReportSettings settings = settingsCaptor.getValue();
        assertThat(settings.getFrequency()).isEqualTo(ReportFrequency.MONTHLY);
        assertThat(settings.isEnabled()).isTrue();
        assertThat(settings.getLastSentDate()).isNull();
        assertThat(settings.getNextReportDate()).isNotNull();
    }

    // ── login ────────────────────────────────────────────────────────────────

    @Test
    void login_emailNotFound_throwsUnauthorizedWithGenericMessage() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        UserLoginRequest request = new UserLoginRequest("missing@example.com", "password123");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Incorrect email or password");
    }

    @Test
    void login_passwordMismatch_throwsUnauthorizedWithSameGenericMessage() {
        User user = userWithId("jane@example.com", "hashed");
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpwd", "hashed")).thenReturn(false);
        UserLoginRequest request = new UserLoginRequest("jane@example.com", "wrongpwd");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Incorrect email or password");
    }

    @Test
    void login_success_withoutReportSettings_returnsNullReportSettingsInResponse() {
        User user = userWithId("jane@example.com", "hashed");
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.createAccessToken(user.getIdAsString())).thenReturn("access-token");
        when(jwtService.createRefreshToken(user.getIdAsString())).thenReturn("refresh-token");
        when(jwtService.getAccessTokenTtl()).thenReturn(Duration.ofMinutes(15));
        when(jwtService.getRefreshTokenTtl()).thenReturn(Duration.ofDays(7));
        when(reportSettingsRepository.findByUserId(user.getId())).thenReturn(Optional.empty());

        UserLoginResponse response = authService.login(new UserLoginRequest("jane@example.com", "password123"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.reportSettings()).isNull();
    }

    @Test
    void login_success_withReportSettings_mapsSettingsIntoResponse() {
        User user = userWithId("jane@example.com", "hashed");
        ReportSettings settings = new ReportSettings();
        settings.setId(new ObjectId());
        settings.setFrequency(ReportFrequency.MONTHLY);
        settings.setEnabled(true);

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.createAccessToken(any())).thenReturn("access-token");
        when(jwtService.createRefreshToken(any())).thenReturn("refresh-token");
        when(jwtService.getAccessTokenTtl()).thenReturn(Duration.ofMinutes(15));
        when(jwtService.getRefreshTokenTtl()).thenReturn(Duration.ofDays(7));
        when(reportSettingsRepository.findByUserId(user.getId())).thenReturn(Optional.of(settings));

        UserLoginResponse response = authService.login(new UserLoginRequest("jane@example.com", "password123"));

        assertThat(response.reportSettings()).isNotNull();
        assertThat(response.reportSettings().isEnabled()).isTrue();
    }

    // ── refreshToken ─────────────────────────────────────────────────────────

    @Test
    void refreshToken_invalidOrExpiredToken_throwsUnauthorized() {
        when(jwtService.verifyToken("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken("bad-token"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid or expired refresh token");
    }

    @Test
    void refreshToken_subjectNotAValidObjectId_throwsUserNotFound() {
        when(jwtService.verifyToken("token")).thenReturn(Optional.of("not-an-object-id"));

        assertThatThrownBy(() -> authService.refreshToken("token"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void refreshToken_userNoLongerExists_throwsUserNotFound() {
        ObjectId id = new ObjectId();
        when(jwtService.verifyToken("token")).thenReturn(Optional.of(id.toHexString()));
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken("token"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void refreshToken_success_returnsNewAccessTokenAndTtl() {
        ObjectId id = new ObjectId();
        User user = userWithId("jane@example.com", "hashed");
        user.setId(id);
        when(jwtService.verifyToken("token")).thenReturn(Optional.of(id.toHexString()));
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(jwtService.createAccessToken(id.toHexString())).thenReturn("new-access-token");
        when(jwtService.getAccessTokenTtl()).thenReturn(Duration.ofMinutes(15));

        RefreshTokenResponse response = authService.refreshToken("token");

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.expiresIn()).isEqualTo(Duration.ofMinutes(15).toMillis());
    }

    // ── sendForgotPasswordOtp ────────────────────────────────────────────────

    @Test
    void sendForgotPasswordOtp_userNotFound_throwsNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.sendForgotPasswordOtp("missing@example.com"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void sendForgotPasswordOtp_existingUnexpiredOtp_throwsBadRequest() {
        User user = userWithId("jane@example.com", "hashed");
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(otpRepository.findFirstByEmailAndExpiresAtGreaterThan(eq("jane@example.com"), any()))
                .thenReturn(Optional.of(new ForgotPasswordOtp()));

        assertThatThrownBy(() -> authService.sendForgotPasswordOtp("jane@example.com"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already been sent");

        verify(forgotPasswordMailer, never()).send(any(), any(), any());
    }

    @Test
    void sendForgotPasswordOtp_success_savesOtpAndSendsEmail() {
        User user = userWithId("jane@example.com", "hashed");
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(otpRepository.findFirstByEmailAndExpiresAtGreaterThan(eq("jane@example.com"), any()))
                .thenReturn(Optional.empty());
        when(otpRepository.save(any(ForgotPasswordOtp.class))).thenAnswer(invocation -> {
            ForgotPasswordOtp otp = invocation.getArgument(0);
            otp.setId(new ObjectId());
            return otp;
        });

        authService.sendForgotPasswordOtp("jane@example.com");

        ArgumentCaptor<ForgotPasswordOtp> captor = ArgumentCaptor.forClass(ForgotPasswordOtp.class);
        verify(otpRepository).save(captor.capture());
        assertThat(captor.getValue().getOtp()).matches("\\d{6}");
        assertThat(captor.getValue().isVerified()).isFalse();
        verify(forgotPasswordMailer).send("jane@example.com", "Jane Doe", captor.getValue().getOtp());
    }

    @Test
    void sendForgotPasswordOtp_savedOtpWithoutId_throwsInternalError() {
        User user = userWithId("jane@example.com", "hashed");
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(otpRepository.findFirstByEmailAndExpiresAtGreaterThan(eq("jane@example.com"), any()))
                .thenReturn(Optional.empty());
        when(otpRepository.save(any(ForgotPasswordOtp.class))).thenReturn(new ForgotPasswordOtp());

        assertThatThrownBy(() -> authService.sendForgotPasswordOtp("jane@example.com"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        verify(forgotPasswordMailer, never()).send(any(), any(), any());
    }

    // ── verifyForgotPasswordOtp ──────────────────────────────────────────────

    @Test
    void verifyForgotPasswordOtp_noMatchingRecord_throwsBadRequest() {
        when(otpRepository.findFirstByEmailAndOtpAndExpiresAtGreaterThanAndVerified(
                eq("jane@example.com"), eq("123456"), any(), eq(false)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyForgotPasswordOtp("jane@example.com", "123456"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid or expired OTP");
    }

    @Test
    void verifyForgotPasswordOtp_success_marksVerifiedAndReturnsTrue() {
        ForgotPasswordOtp record = new ForgotPasswordOtp();
        record.setId(new ObjectId());
        record.setVerified(false);
        when(otpRepository.findFirstByEmailAndOtpAndExpiresAtGreaterThanAndVerified(
                eq("jane@example.com"), eq("123456"), any(), eq(false)))
                .thenReturn(Optional.of(record));

        boolean result = authService.verifyForgotPasswordOtp("jane@example.com", "123456");

        assertThat(result).isTrue();
        assertThat(record.isVerified()).isTrue();
        verify(otpRepository).save(record);
    }

    // ── resetPassword ────────────────────────────────────────────────────────

    @Test
    void resetPassword_noVerifiedOtp_throwsBadRequest() {
        when(otpRepository.findFirstByEmailAndExpiresAtGreaterThanAndVerified(
                eq("jane@example.com"), any(), eq(true)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword("jane@example.com", "newpassword1"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("OTP verification required");
    }

    @Test
    void resetPassword_userMissingAfterOtpVerified_throwsInternalError() {
        ForgotPasswordOtp record = new ForgotPasswordOtp();
        record.setVerified(true);
        when(otpRepository.findFirstByEmailAndExpiresAtGreaterThanAndVerified(
                eq("jane@example.com"), any(), eq(true)))
                .thenReturn(Optional.of(record));
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword("jane@example.com", "newpassword1"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void resetPassword_success_updatesPasswordAndInvalidatesOtp() {
        ForgotPasswordOtp record = new ForgotPasswordOtp();
        record.setVerified(true);
        User user = userWithId("jane@example.com", "oldHash");
        when(otpRepository.findFirstByEmailAndExpiresAtGreaterThanAndVerified(
                eq("jane@example.com"), any(), eq(true)))
                .thenReturn(Optional.of(record));
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpassword1")).thenReturn("newHash");

        authService.resetPassword("jane@example.com", "newpassword1");

        assertThat(user.getPassword()).isEqualTo("newHash");
        assertThat(record.isVerified()).isFalse();
        verify(userRepository).save(user);
        verify(otpRepository, times(1)).save(record);
    }
}
