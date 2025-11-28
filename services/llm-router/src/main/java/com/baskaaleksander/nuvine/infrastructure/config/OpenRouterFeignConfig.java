package com.baskaaleksander.nuvine.infrastructure.config;


import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class OpenRouterFeignConfig {

    @Value("${openrouter.api-key}")
    private String apiKey;

    @Bean
    public RequestInterceptor openRouterAuthInterceptor() {
        return template -> {
            template.header("Authorization", "Bearer " + apiKey);
            template.header("HTTP-Referer", "https://nuvine.org");
            template.header("X-Title", "Nuvine LLM Router");
            template.header("Content-Type", "application/json");
        };
    }
}
