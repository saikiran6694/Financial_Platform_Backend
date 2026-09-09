package com.arthium.finance.mail;

import com.arthium.finance.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Port of mailers/mailer.py — sends through the Resend HTTP API.
 */
@Service
public class MailerService {

    private static final Logger log = LoggerFactory.getLogger(MailerService.class);

    private final RestClient restClient;
    private final AppProperties properties;

    public MailerService(RestClient outboundRestClient, AppProperties properties) {
        this.restClient = outboundRestClient;
        this.properties = properties;
    }

    public void send(String to, String subject, String text, String html) {
        send(List.of(to), subject, text, html);
    }

    public void send(List<String> recipients, String subject, String text, String html) {
        AppProperties.Resend config = properties.getResend();
        String from = config.getSenderName() + " <" + config.getSender() + ">";

        Map<String, Object> body = Map.of(
                "from", from,
                "to", recipients,
                "subject", subject,
                "text", text == null ? "" : text,
                "html", html == null ? "" : html
        );

        restClient.post()
                .uri(config.getBaseUrl() + "/emails")
                .header("Authorization", "Bearer " + config.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        log.info("Email sent to {} - {}", recipients, subject);
    }

    public static String formatCurrency(double amount) {
        return String.format(Locale.US, "$%,.2f", amount);
    }
}
