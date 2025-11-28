package com.baskaaleksander.nuvine.infrastructure.config;

import feign.RequestInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class InternalFeignConfig {

    // as the only allowed to use these routes is internal service, we can forward the token
    @Bean
    public RequestInterceptor userTokenForwardingInterceptor() {
        return template -> {
            var auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                String tokenValue = jwtAuth.getToken().getTokenValue();
                template.header("Authorization", "Bearer " + tokenValue);
            }
        };
    }
}
