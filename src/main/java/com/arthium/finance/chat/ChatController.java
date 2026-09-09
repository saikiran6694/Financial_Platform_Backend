package com.arthium.finance.chat;

import com.arthium.finance.chat.dto.ChatRequest;
import com.arthium.finance.chat.dto.ChatResponse;
import com.arthium.finance.common.ApiException;
import com.arthium.finance.user.User;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final GroqChatService groqChatService;

    public ChatController(GroqChatService groqChatService) {
        this.groqChatService = groqChatService;
    }

    @PostMapping({"", "/"})
    public ChatResponse chat(@Valid @RequestBody ChatRequest request,
                             @AuthenticationPrincipal User currentUser) {
        try {
            Map<String, Object> result = groqChatService.runChat(
                    currentUser.getIdAsString(),
                    currentUser.getName(),
                    request.message(),
                    request.history());

            return new ChatResponse(true, result);

        } catch (Exception e) {
            log.error("Chat error for user {}", currentUser.getIdAsString(), e);
            throw ApiException.internal("Failed to process your query. Please try again.");
        }
    }
}
