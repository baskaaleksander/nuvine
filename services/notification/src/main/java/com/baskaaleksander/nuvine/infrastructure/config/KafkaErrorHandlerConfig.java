package com.baskaaleksander.nuvine.infrastructure.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaErrorHandlerConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> template) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                template,
                (record, ex) -> {
                    String dltTopic = record.topic() + ".DLT";
                    return new TopicPartition(dltTopic, record.partition());
                }
        );

        return new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(2000L, 3L)
        );
    }

}
