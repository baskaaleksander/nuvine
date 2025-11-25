package com.baskaaleksander.nuvine.infrastructure.provider;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class OpenAiProviderClient {

    @Value("${api-key.openai}")
    private String apiKey;

    @Bean
    public OpenAIClient getClient() {
        return OpenAIOkHttpClient.builder()
                .fromEnv()
                .apiKey(apiKey)
                .build();
    }
}
