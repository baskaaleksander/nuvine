package com.baskaaleksander.nuvine.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfiguration {

    @Value("${topics.workspace-member-added-topic}")
    private String workspaceMemberAddedTopic;

    @Value("${topics.document-uploaded-topic}")
    private String documentUploadedTopic;

    @Bean
    public NewTopic memberAddedTopic() {
        return TopicBuilder
                .name(workspaceMemberAddedTopic)
                .build();
    }

    @Bean
    public NewTopic documentUploadedTopic() {
        return TopicBuilder
                .name(documentUploadedTopic)
                .build();
    }

}
