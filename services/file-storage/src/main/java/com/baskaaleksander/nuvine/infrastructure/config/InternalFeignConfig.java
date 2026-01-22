package com.baskaaleksander.nuvine.infrastructure.config;

import com.baskaaleksander.nuvine.infrastructure.auth.KeycloakClientCredentialsTokenProvider;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!integrationtest")
public class InternalFeignConfig {

    private final KeycloakClientCredentialsTokenProvider tokenProvider;

    public InternalFeignConfig(KeycloakClientCredentialsTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Bean
    public RequestInterceptor internalTokenForwardingInterceptor() {

        return template -> {
            var token = tokenProvider.getAccessToken();
            template.header("Authorization", "Bearer " + token);
        };

    }
}
