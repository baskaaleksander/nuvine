package com.baskaaleksander.nuvine.infrastructure.config;

import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

@Configuration
public class OpenAIConfig {

    @Bean
    public RestTemplate openAiRestTemplate(OpenAIProperties props) {
        RestTemplate restTemplate = new RestTemplate();

        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().setBearerAuth(props.apiKey());
            request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return execution.execute(request, body);
        });

        return restTemplate;
    }

    @Bean
    @ConfigurationProperties(prefix = "openai")
    public OpenAIProperties openAIProperties() {
        return new OpenAIProperties();
    }

    @Setter
    public static class OpenAIProperties {
        private String baseUrl;
        private String apiKey;
        private String embeddingModel;


        public String baseUrl() {
            return baseUrl;
        }

        public String apiKey() {
            return apiKey;
        }

        public String embeddingModel() {
            return embeddingModel;
        }

    }
}