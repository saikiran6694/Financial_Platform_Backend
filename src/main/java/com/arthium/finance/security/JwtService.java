package com.arthium.finance.security;

import com.arthium.finance.config.AppProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of services/auth.py token handling.
 *
 * Implemented directly on javax.crypto.Mac rather than a JWT library so the
 * tokens are byte-for-byte compatible with the ones PyJWT issued: same header
 * ({"alg":"HS256","typ":"JWT"}), same claims ({"sub", "exp"}), same secret.
 * Tokens minted by the Python service stay valid after the cutover.
 */
@Service
public class JwtService {

    private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final Pattern SUB_PATTERN = Pattern.compile("\"sub\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern EXP_PATTERN = Pattern.compile("\"exp\"\\s*:\\s*(\\d+)");

    private final byte[] secret;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;

    public JwtService(AppProperties properties) {
        if (properties.getSecretKey() == null || properties.getSecretKey().isBlank()) {
            throw new IllegalStateException("SECRET_KEY must be configured");
        }
        if (!"HS256".equalsIgnoreCase(properties.getAlgorithm())) {
            throw new IllegalStateException("Only HS256 is supported, got: " + properties.getAlgorithm());
        }
        this.secret = properties.getSecretKey().getBytes(StandardCharsets.UTF_8);
        this.accessTokenTtl = Duration.ofMinutes(properties.getAccessTokenExpireMinutes());
        this.refreshTokenTtl = Duration.ofMinutes(properties.getRefreshTokenExpireMinutes());
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public Duration getRefreshTokenTtl() {
        return refreshTokenTtl;
    }

    public String createAccessToken(String subject) {
        return createToken(subject, accessTokenTtl);
    }

    public String createRefreshToken(String subject) {
        return createToken(subject, refreshTokenTtl);
    }

    public String createToken(String subject, Duration expiresIn) {
        long exp = Instant.now().plus(expiresIn).getEpochSecond();
        String payloadJson = "{\"sub\":\"" + escape(subject) + "\",\"exp\":" + exp + "}";

        String header = base64Url(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
        String payload = base64Url(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signingInput = header + "." + payload;
        String signature = base64Url(hmacSha256(signingInput.getBytes(StandardCharsets.UTF_8)));

        return signingInput + "." + signature;
    }

    /**
     * Verifies signature + expiry and returns the "sub" claim.
     * Mirrors verify_access_token, which returns None on any failure.
     */
    public Optional<String> verifyToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return Optional.empty();
        }

        try {
            String signingInput = parts[0] + "." + parts[1];
            byte[] expected = hmacSha256(signingInput.getBytes(StandardCharsets.UTF_8));
            byte[] provided = Base64.getUrlDecoder().decode(parts[2]);

            if (!MessageDigest.isEqual(expected, provided)) {
                return Optional.empty();
            }

            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

            Matcher expMatcher = EXP_PATTERN.matcher(payloadJson);
            if (!expMatcher.find()) {
                return Optional.empty();
            }
            long exp = Long.parseLong(expMatcher.group(1));
            if (Instant.now().getEpochSecond() >= exp) {
                return Optional.empty();
            }

            Matcher subMatcher = SUB_PATTERN.matcher(payloadJson);
            if (!subMatcher.find()) {
                return Optional.empty();
            }
            String subject = subMatcher.group(1);
            return subject.isBlank() ? Optional.empty() : Optional.of(subject);

        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private byte[] hmacSha256(byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign JWT", e);
        }
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
