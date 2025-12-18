package com.baskaaleksander.nuvine.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${topics.embedding-request-topic}")
    private String embeddingRequestTopic;

    @Value("${topics.vector-processing-completed-topic}")
    private String vectorProcessingCompletedTopic;

    @Value("${topics.embedding-completed-dlq-topic}")
    private String embeddingCompletedDlqTopic;

    @Value("${topics.embedding-completed-dead-letter-topic}")
    private String embeddingCompletedDeadLetterTopic;

    @Bean
    public NewTopic embeddingRequestTopic() {
        return TopicBuilder
                .name(embeddingRequestTopic)
                .build();
    }

    @Bean
    public NewTopic vectorProcessingCompletedTopic() {
        return TopicBuilder
                .name(vectorProcessingCompletedTopic)
                .build();
    }

    @Bean
    public NewTopic embeddingCompletedDlqTopic() {
        return TopicBuilder
                .name(embeddingCompletedDlqTopic)
                .build();
    }

    @Bean
    public NewTopic embeddingCompletedDeadLetterTopic() {
        return TopicBuilder
                .name(embeddingCompletedDeadLetterTopic)
                .build();
    }
}
