package com.arthium.finance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Mirrors the Settings class from config/config.py.
 * Bound from application.yml, which in turn reads the same environment
 * variable names the Python service used.
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String secretKey;
    private String algorithm = "HS256";
    private long accessTokenExpireMinutes = 60;
    private long refreshTokenExpireMinutes = 60 * 24 * 7;
    private String allowedOrigins = "";
    private String defaultTimezone = "America/St_Johns";

    private Cloudinary cloudinary = new Cloudinary();
    private Gemini gemini = new Gemini();
    private Groq groq = new Groq();
    private Resend resend = new Resend();

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

    public long getAccessTokenExpireMinutes() { return accessTokenExpireMinutes; }
    public void setAccessTokenExpireMinutes(long v) { this.accessTokenExpireMinutes = v; }

    public long getRefreshTokenExpireMinutes() { return refreshTokenExpireMinutes; }
    public void setRefreshTokenExpireMinutes(long v) { this.refreshTokenExpireMinutes = v; }

    public String getAllowedOrigins() { return allowedOrigins; }
    public void setAllowedOrigins(String allowedOrigins) { this.allowedOrigins = allowedOrigins; }

    public String getDefaultTimezone() { return defaultTimezone; }
    public void setDefaultTimezone(String defaultTimezone) { this.defaultTimezone = defaultTimezone; }

    public Cloudinary getCloudinary() { return cloudinary; }
    public void setCloudinary(Cloudinary cloudinary) { this.cloudinary = cloudinary; }

    public Gemini getGemini() { return gemini; }
    public void setGemini(Gemini gemini) { this.gemini = gemini; }

    public Groq getGroq() { return groq; }
    public void setGroq(Groq groq) { this.groq = groq; }

    public Resend getResend() { return resend; }
    public void setResend(Resend resend) { this.resend = resend; }

    public static class Cloudinary {
        private String cloudName;
        private String apiKey;
        private String apiSecret;

        public String getCloudName() { return cloudName; }
        public void setCloudName(String cloudName) { this.cloudName = cloudName; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getApiSecret() { return apiSecret; }
        public void setApiSecret(String apiSecret) { this.apiSecret = apiSecret; }
    }

    public static class Gemini {
        private String apiKey;
        private String model = "gemini-2.0-flash";
        private String baseUrl = "https://generativelanguage.googleapis.com";

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    }

    public static class Groq {
        private String apiKey;
        private String model = "openai/gpt-oss-20b";
        private String baseUrl = "https://api.groq.com";

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    }

    public static class Resend {
        private String apiKey;
        private String sender;
        private String senderName = "Saikiran";
        private String baseUrl = "https://api.resend.com";

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getSender() { return sender; }
        public void setSender(String sender) { this.sender = sender; }
        public String getSenderName() { return senderName; }
        public void setSenderName(String senderName) { this.senderName = senderName; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    }
}
