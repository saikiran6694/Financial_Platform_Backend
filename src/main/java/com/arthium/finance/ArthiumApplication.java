package com.arthium.finance;

import com.arthium.finance.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

// Auth is fully custom (JWT + MongoDB via UserRepository/AuthService), so the
// default in-memory UserDetailsService is unused — excluded to stop Spring Boot
// from generating and logging a random dev password on every startup.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableConfigurationProperties(AppProperties.class)
public class ArthiumApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArthiumApplication.class, args);
    }
}
