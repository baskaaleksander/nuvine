package com.baskaaleksander.nuvine.integration.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("integrationtest")
public class IntegrationFeignTestConfig {

    @Bean
    public RequestInterceptor integrationTestInterceptor() {
        return template -> template.header("Authorization", "Bearer integration-test-token");
    }
}
