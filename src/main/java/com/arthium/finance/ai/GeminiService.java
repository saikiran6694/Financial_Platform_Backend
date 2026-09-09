package com.arthium.finance.ai;

import com.arthium.finance.common.Json;
import com.arthium.finance.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Port of config/google_genai_config.py.
 * Calls the Gemini generateContent REST endpoint directly rather than pulling
 * in the google-genai SDK.
 */
@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private final RestClient restClient;
    private final AppProperties properties;
    private final Json json;

    public GeminiService(RestClient outboundRestClient, AppProperties properties, Json json) {
        this.restClient = outboundRestClient;
        this.properties = properties;
        this.json = json;
    }

    /** Sends an image plus a prompt to Gemini and returns the raw response text. */
    public String generateContent(String prompt, byte[] fileBytes, String mimeType) {
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(
                                Map.of("text", prompt),
                                Map.of("inline_data", Map.of(
                                        "mime_type", mimeType != null ? mimeType : "image/jpeg",
                                        "data", Base64.getEncoder().encodeToString(fileBytes)
                                ))
                        )
                )),
                "generationConfig", Map.of(
                        "temperature", 0,
                        "topP", 1,
                        "responseMimeType", "application/json"
                )
        );

        return callGemini(body);
    }

    /** Port of generate_ai_insights: exactly three insight strings, or an empty list. */
    public List<String> generateAiInsights(double totalIncome,
                                           double totalExpenses,
                                           double availableBalance,
                                           double savingsRate,
                                           Map<String, Map<String, Object>> categories,
                                           String periodLabel) {

        String prompt = Prompts.reportInsightPrompt(
                totalIncome, totalExpenses, availableBalance, savingsRate, categories, periodLabel);

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of(
                        "temperature", 0.7,
                        "topP", 1,
                        "responseMimeType", "application/json"
                )
        );

        try {
            String raw = Json.stripCodeFences(callGemini(body));
            List<Object> parsed = json.readList(raw);
            if (parsed == null || parsed.size() != 3) {
                return List.of();
            }
            List<String> insights = new ArrayList<>();
            for (Object item : parsed) {
                insights.add(String.valueOf(item));
            }
            return insights;
        } catch (Exception e) {
            log.error("Gemini insight generation failed", e);
            return List.of();
        }
    }

    private String callGemini(Map<String, Object> body) {
        String url = properties.getGemini().getBaseUrl()
                + "/v1beta/models/" + properties.getGemini().getModel() + ":generateContent";

        String response = restClient.post()
                .uri(url)
                .header("x-goog-api-key", properties.getGemini().getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        Map<String, Object> root = json.readMap(response);
        if (root == null) {
            return "";
        }

        List<Object> candidates = Json.asList(root.get("candidates"));
        if (candidates == null || candidates.isEmpty()) {
            return "";
        }

        Map<String, Object> content = Json.asMap(Json.get(Json.asMap(candidates.get(0)), "content"));
        List<Object> parts = Json.asList(Json.get(content, "parts"));
        if (parts == null) {
            return "";
        }

        StringBuilder text = new StringBuilder();
        for (Object part : parts) {
            String value = Json.str(Json.asMap(part), "text");
            if (value != null) {
                text.append(value);
            }
        }
        return text.toString();
    }
}
