package com.baskaaleksander.nuvine.infrastructure.messaging.out;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.LogTokenUsageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LogTokenUsageEventProducer {

    private final KafkaTemplate<String, LogTokenUsageEvent> kafkaTemplate;

    @Value("${topics.log-token-usage-topic}")
    private String topic;

    public void produceLogTokenUsageEvent(LogTokenUsageEvent event) {
        Message<LogTokenUsageEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, "log-token-usage-topic")
                .build();
        kafkaTemplate.send(message);
    }
}
