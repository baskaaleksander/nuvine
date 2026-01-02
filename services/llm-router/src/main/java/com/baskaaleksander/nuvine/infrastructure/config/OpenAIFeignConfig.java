package com.baskaaleksander.nuvine.infrastructure.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class OpenAIFeignConfig {

    @Value("${openai.api-key}")
    private String apiKey;

    @Bean
    public RequestInterceptor openAIAuthInterceptor() {
        return template -> {
            template.header("Authorization", "Bearer " + apiKey);
            template.header("Content-Type", "application/json");
        };
    }
}
