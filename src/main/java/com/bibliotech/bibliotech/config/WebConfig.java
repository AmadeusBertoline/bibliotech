package com.bibliotech.bibliotech.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@Configuration
public class WebConfig {

    @Bean
    PageableHandlerMethodArgumentResolverCustomizer pageableSizeCustomizer(
            @Value("${spring.data.web.pageable.max-page-size:2000}") int maxPageSize) {
        return resolver -> resolver.setMaxPageSize(maxPageSize);
    }
}
