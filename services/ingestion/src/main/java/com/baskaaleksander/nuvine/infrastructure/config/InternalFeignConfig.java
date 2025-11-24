package com.baskaaleksander.nuvine.infrastructure.config;

import com.baskaaleksander.nuvine.infrastructure.auth.KeycloakClientCredentialsTokenProvider;
import feign.RequestInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class InternalFeignConfig {

    private final KeycloakClientCredentialsTokenProvider tokenProvider;

    @Bean
    public RequestInterceptor internalTokenForwardingInterceptor() {

        return template -> {
            var token = tokenProvider.getAccessToken();
            template.header("Authorization", "Bearer " + token);
        };

    }
}
