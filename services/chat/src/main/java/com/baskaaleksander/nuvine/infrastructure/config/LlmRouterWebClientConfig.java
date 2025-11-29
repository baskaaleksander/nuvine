package com.baskaaleksander.nuvine.infrastructure.config;

import com.baskaaleksander.nuvine.infrastructure.auth.KeycloakClientCredentialsTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
public class LlmRouterWebClientConfig {

    private final KeycloakClientCredentialsTokenProvider tokenProvider;

    @Bean
    public WebClient llmRouterWebClient() {

        String token = tokenProvider.getAccessToken();

        return WebClient.builder()
                .baseUrl("http://localhost:8090")
                .defaultHeader("Authorization", "Bearer " + token)
                .build();
    }
}
