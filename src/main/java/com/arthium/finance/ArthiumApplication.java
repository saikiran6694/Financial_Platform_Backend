package com.arthium.finance;

import com.arthium.finance.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class ArthiumApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArthiumApplication.class, args);
    }
}
