package com.baskaaleksander.nuvine.infrastructure.messaging;

import com.baskaaleksander.nuvine.application.util.MaskingUtil;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.PasswordResetEvent;
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

    public void sendPasswordResetEvent(PasswordResetEvent event) {
        Message<PasswordResetEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, passwordResetTopic)
                .build();

        kafkaTemplate.send(message);

        log.info("Password reset event sent: {}", MaskingUtil.maskEmail(event.email()));
    }
}
