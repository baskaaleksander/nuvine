package com.baskaaleksander.nuvine.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${topics.embedding-completed-topic}")
    private String embeddingCompletedTopic;

    @Bean
    public NewTopic embeddingCompletedTopic() {
        return TopicBuilder
                .name(embeddingCompletedTopic)
                .build();
    }
}
