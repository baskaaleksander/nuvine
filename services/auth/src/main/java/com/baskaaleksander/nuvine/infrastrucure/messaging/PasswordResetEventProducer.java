package com.baskaaleksander.nuvine.infrastrucure.messaging;

import com.baskaaleksander.nuvine.infrastrucure.messaging.dto.PasswordResetEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PasswordResetEventProducer {

    @Value("${topics.password-reset-topic}")
    private String passwordResetTopic;

    private final KafkaTemplate<String, PasswordResetEvent> kafkaTemplate;

    private void sendPasswordResetEvent(PasswordResetEvent event) {
        Message<PasswordResetEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, passwordResetTopic)
                .build();

        kafkaTemplate.send(message);
    }
}
