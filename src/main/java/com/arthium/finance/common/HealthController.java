package com.arthium.finance.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> checkHealth() {
        return Map.of("server", "Server running as of " + LocalDateTime.now());
    }
}
