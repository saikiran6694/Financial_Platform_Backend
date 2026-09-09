package com.arthium.finance.config;

import com.arthium.finance.report.DateRange;
import com.arthium.finance.transaction.RecurringStatus;
import com.arthium.finance.transaction.TransactionType;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Query-parameter converters for the enums whose wire values do not match their
 * Java constant names (e.g. preset=30days, preset=lastMonth).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, DateRange.class, DateRange::fromValue);
        registry.addConverter(String.class, TransactionType.class,
                source -> TransactionType.valueOf(source.trim().toUpperCase()));
        registry.addConverter(String.class, RecurringStatus.class,
                source -> RecurringStatus.valueOf(source.trim().toUpperCase()));
    }
}
