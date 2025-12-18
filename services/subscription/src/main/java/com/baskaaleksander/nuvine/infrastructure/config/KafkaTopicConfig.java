package com.baskaaleksander.nuvine.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${topics.payment-action-required-topic}")
    private String paymentActionRequiredTopic;

    @Value("${topics.log-token-usage-topic}")
    private String logTokenUsageTopic;

    @Value("${topics.usage-logs-dlq-topic}")
    private String usageLogsDlqTopic;

    @Value("${topics.usage-logs-dead-letter-topic}")
    private String usageLogsDeadLetterTopic;

    @Bean
    public NewTopic paymentActionRequiredTopic() {
        return TopicBuilder
                .name(paymentActionRequiredTopic)
                .build();
    }

    @Bean
    public NewTopic logTokenUsageTopic() {
        return TopicBuilder
                .name(logTokenUsageTopic)
                .build();
    }

    @Bean
    public NewTopic usageLogsDlqTopic() {
        return TopicBuilder
                .name(usageLogsDlqTopic)
                .build();
    }

    @Bean
    public NewTopic usageLogsDeadLetterTopic() {
        return TopicBuilder
                .name(usageLogsDeadLetterTopic)
                .build();
    }
}
