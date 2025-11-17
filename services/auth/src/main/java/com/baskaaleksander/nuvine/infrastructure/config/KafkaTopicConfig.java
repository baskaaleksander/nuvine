package com.baskaaleksander.nuvine.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${topics.email-verification-topic}")
    private String emailVerificationTopic;

    @Value("${topics.password-reset-topic}")
    private String passwordResetTopic;

    @Value("${topics.user-registered-topic}")
    private String userRegisteredTopic;

    @Bean
    public NewTopic emailVerificationTopic() {
        return TopicBuilder
                .name(emailVerificationTopic)
                .build();
    }

    @Bean
    public NewTopic passwordResetTopic() {
        return TopicBuilder
                .name(passwordResetTopic)
                .build();
    }

    @Bean
    public NewTopic userRegisteredTopic() {
        return TopicBuilder
                .name(userRegisteredTopic)
                .build();
    }

}
