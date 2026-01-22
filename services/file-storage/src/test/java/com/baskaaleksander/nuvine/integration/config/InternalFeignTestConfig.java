package com.baskaaleksander.nuvine.integration.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("integrationtest")
public class InternalFeignTestConfig {

    @Bean
    public RequestInterceptor internalTokenForwardingInterceptor() {
        return template -> {
        };
    }
}
