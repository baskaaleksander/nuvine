package com.baskaaleksander.nuvine.infrastructure.config;

import com.baskaaleksander.nuvine.infrastructure.auth.KeycloakClientCredentialsTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class LlmRouterWebClientConfig {

    private final KeycloakClientCredentialsTokenProvider tokenProvider;
    private final String apiBaseUrl;

    public LlmRouterWebClientConfig(
            KeycloakClientCredentialsTokenProvider tokenProvider,
            @Value("${application.config.api-base-url}") String apiBaseUrl
    ) {
        this.tokenProvider = tokenProvider;
        this.apiBaseUrl = apiBaseUrl;
    }

    @Bean
    public WebClient llmRouterWebClient() {

        return WebClient.builder()
                .baseUrl(apiBaseUrl)
                .filter(authorizationFilter())
                .build();
    }

    private ExchangeFilterFunction authorizationFilter() {
        return (request, next) -> {
            String token = tokenProvider.getAccessToken();

            ClientRequest newRequest = ClientRequest.from(request)
                    .headers(headers -> headers.setBearerAuth(token))
                    .build();

            return next.exchange(newRequest);
        };
    }
}
