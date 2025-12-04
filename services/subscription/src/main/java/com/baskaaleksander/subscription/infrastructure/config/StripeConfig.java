package com.baskaaleksander.subscription.infrastructure.config;

import com.stripe.StripeClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {

    @Value("${stripe.api-key}")
    private String stripeApiKey;

    @Bean
    public StripeClient getStripeClient() {
        return StripeClient.builder().setApiKey(stripeApiKey).build();
    }
}
