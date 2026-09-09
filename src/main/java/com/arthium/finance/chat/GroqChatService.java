package com.arthium.finance.chat;

import com.arthium.finance.chat.dto.ChatHistoryItem;
import com.arthium.finance.common.Json;
import com.arthium.finance.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of services/chat.py — the agentic loop against Groq's
 * OpenAI-compatible chat completions API.
 *
 * 1. Build system prompt + history + user message
 * 2. Send to Groq with the tool definitions
 * 3. If the model calls tools, run them and feed the results back as role="tool"
 * 4. Repeat until the model answers, then parse its structured JSON
 */
@Service
public class GroqChatService {

    private static final Logger log = LoggerFactory.getLogger(GroqChatService.class);
    private static final int MAX_TOOL_ROUNDS = 6;

    private final RestClient restClient;
    private final AppProperties properties;
    private final ChatQueryService queryService;
    private final Json json;

    public GroqChatService(RestClient outboundRestClient,
                           AppProperties properties,
                           ChatQueryService queryService,
                           Json json) {
        this.restClient = outboundRestClient;
        this.properties = properties;
        this.queryService = queryService;
        this.json = json;
    }

    public Map<String, Object> runChat(String userId,
                                       String username,
                                       String message,
                                       List<ChatHistoryItem> history) {

        String systemPrompt = ChatPrompts.toolSystemPrompt(Instant.now(), username);
        List<Map<String, Object>> tools = ChatPrompts.toolDefinitions();

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.<String, Object>of("role", "system", "content", systemPrompt));

        for (ChatHistoryItem turn : history) {
            messages.add(Map.<String, Object>of("role", turn.role(), "content", turn.content()));
        }
        messages.add(Map.<String, Object>of("role", "user", "content", message));

        String rawText = null;
        boolean usedTools = false;

        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            Map<String, Object> assistantMessage = callGroq(messages, tools);

            if (assistantMessage == null) {
                break;
            }

            // Always append the assistant turn so the conversation stays coherent.
            messages.add(assistantMessage);

            List<Object> toolCalls = Json.asList(assistantMessage.get("tool_calls"));

            if (toolCalls == null || toolCalls.isEmpty()) {
                Object content = assistantMessage.get("content");
                rawText = content == null ? "" : String.valueOf(content);

                if (usedTools) {
                    messages.add(Map.of(
                            "role", "system",
                            "content", ChatPrompts.systemPrompt(Instant.now(), username)
                                    + "\nReturn the final response as JSON in assistant content. "
                                    + "Do not call tools; no tools are available in this final step."));
                    Map<String, Object> finalMessage = callGroq(messages, List.of());
                    if (finalMessage != null && finalMessage.get("content") != null) {
                        rawText = String.valueOf(finalMessage.get("content"));
                    }
                }
                break;
            }

            usedTools = true;

            for (Object rawCall : toolCalls) {
                Map<String, Object> toolCall = Json.asMap(rawCall);
                Map<String, Object> function = Json.asMap(Json.get(toolCall, "function"));

                String toolName = Json.str(function, "name");
                String argumentsJson = Json.str(function, "arguments");

                Map<String, Object> arguments = argumentsJson == null
                        ? Map.of()
                        : json.readMap(argumentsJson);
                if (arguments == null) {
                    arguments = Map.of();
                }

                log.info("Tool call: {} args={}", toolName, arguments);
                String result = executeTool(toolName, userId, arguments);

                Map<String, Object> toolMessage = new LinkedHashMap<>();
                toolMessage.put("role", "tool");
                toolMessage.put("tool_call_id", Json.str(toolCall, "id"));
                toolMessage.put("name", toolName);
                toolMessage.put("content", result);
                messages.add(toolMessage);
            }
        }

        if (rawText == null) {
            rawText = json.write(Map.of(
                    "format", "text",
                    "message", "I ran into an issue processing that query. Please try rephrasing."));
        }

        return parseResponse(rawText);
    }

    private String executeTool(String toolName, String userId, Map<String, Object> arguments) {
        try {
            Object result = queryService.execute(toolName, userId, arguments);
            return json.write(result);
        } catch (Exception e) {
            log.error("Tool {} failed", toolName, e);
            return json.write(Map.of("error", String.valueOf(e.getMessage())));
        }
    }

    private Map<String, Object> callGroq(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getGroq().getModel());
        body.put("messages", messages);
        body.put("tools", tools);
        body.put("tool_choice", "auto");
        body.put("temperature", 0.4);

        String response = restClient.post()
                .uri(properties.getGroq().getBaseUrl() + "/openai/v1/chat/completions")
                .header("Authorization", "Bearer " + properties.getGroq().getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        Map<String, Object> parsed = json.readMap(response);
        List<Object> choices = Json.asList(Json.get(parsed, "choices"));

        if (choices == null || choices.isEmpty()) {
            return null;
        }
        return Json.asMap(Json.get(Json.asMap(choices.get(0)), "message"));
    }

    /**
     * Port of _parse_response: the model should return a JSON object; fall back
     * to a plain-text wrapper when it does not.
     */
    private Map<String, Object> parseResponse(String raw) {
        String cleaned = raw == null ? "" : raw.strip();

        if (cleaned.startsWith("```")) {
            StringBuilder builder = new StringBuilder();
            for (String line : cleaned.split("\n")) {
                if (!line.strip().startsWith("```")) {
                    builder.append(line).append("\n");
                }
            }
            cleaned = builder.toString().strip();
        }

        Map<String, Object> parsed = json.readMap(cleaned);
        if (parsed != null && parsed.containsKey("format")) {
            return parsed;
        }

        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("format", "text");
        fallback.put("message", parsed != null ? json.write(parsed) : cleaned);
        return fallback;
    }
}
