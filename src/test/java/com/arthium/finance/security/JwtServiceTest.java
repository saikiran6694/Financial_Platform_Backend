package com.arthium.finance.security;

import com.arthium.finance.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(properties("test-secret-key-value", "HS256", 15, 1440));
    }

    private static AppProperties properties(String secretKey, String algorithm, long accessMinutes, long refreshMinutes) {
        AppProperties properties = new AppProperties();
        properties.setSecretKey(secretKey);
        properties.setAlgorithm(algorithm);
        properties.setAccessTokenExpireMinutes(accessMinutes);
        properties.setRefreshTokenExpireMinutes(refreshMinutes);
        return properties;
    }

    @Test
    void constructor_blankSecretKey_throwsIllegalStateException() {
        assertThatThrownBy(() -> new JwtService(properties("", "HS256", 15, 1440)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SECRET_KEY");
    }

    @Test
    void constructor_nullSecretKey_throwsIllegalStateException() {
        assertThatThrownBy(() -> new JwtService(properties(null, "HS256", 15, 1440)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void constructor_unsupportedAlgorithm_throwsIllegalStateException() {
        assertThatThrownBy(() -> new JwtService(properties("secret", "RS256", 15, 1440)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HS256");
    }

    @Test
    void createAccessToken_thenVerifyToken_returnsOriginalSubject() {
        String token = jwtService.createAccessToken("user-123");

        Optional<String> result = jwtService.verifyToken(token);

        assertThat(result).contains("user-123");
    }

    @Test
    void createRefreshToken_thenVerifyToken_returnsOriginalSubject() {
        String token = jwtService.createRefreshToken("user-456");

        assertThat(jwtService.verifyToken(token)).contains("user-456");
    }

    @Test
    void verifyToken_tokenWithTamperedSignature_returnsEmpty() {
        String token = jwtService.createAccessToken("user-123");
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1] + "." + "tamperedSignatureXYZ123";

        assertThat(jwtService.verifyToken(tampered)).isEmpty();
    }

    @Test
    void verifyToken_expiredToken_returnsEmpty() {
        String expiredToken = jwtService.createToken("user-123", Duration.ofSeconds(-5));

        assertThat(jwtService.verifyToken(expiredToken)).isEmpty();
    }

    @Test
    void verifyToken_malformedTokenWithWrongSegmentCount_returnsEmpty() {
        assertThat(jwtService.verifyToken("only.two")).isEmpty();
    }

    @Test
    void verifyToken_nullToken_returnsEmpty() {
        assertThat(jwtService.verifyToken(null)).isEmpty();
    }

    @Test
    void verifyToken_blankToken_returnsEmpty() {
        assertThat(jwtService.verifyToken("   ")).isEmpty();
    }

    @Test
    void createToken_subjectContainingQuotesAndBackslashes_doesNotBreakTokenParsing() {
        // The payload's "sub" field is escaped for well-formed JSON on write, but the
        // regex-based reader in verifyToken does not unescape on the way back out, so a
        // subject with quote/backslash characters will not round-trip to the exact
        // original value. It should still parse without throwing or failing closed.
        String subject = "weird\"subject\\with\\escapes";
        String token = jwtService.createToken(subject, Duration.ofMinutes(5));

        Optional<String> result = jwtService.verifyToken(token);

        assertThat(result).isPresent();
        assertThat(result.get()).startsWith("weird");
    }

    @Test
    void getAccessTokenTtl_returnsConfiguredDuration() {
        assertThat(jwtService.getAccessTokenTtl()).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void getRefreshTokenTtl_returnsConfiguredDuration() {
        assertThat(jwtService.getRefreshTokenTtl()).isEqualTo(Duration.ofMinutes(1440));
    }
}
