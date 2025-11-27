package com.baskaaleksander.nuvine.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${topics.vector-processing-request-topic}")
    private String vectorProcessingRequestTopic;

    @Bean
    public NewTopic vectorProcessingRequestTopic() {
        return TopicBuilder
                .name(vectorProcessingRequestTopic)
                .build();
    }
}
