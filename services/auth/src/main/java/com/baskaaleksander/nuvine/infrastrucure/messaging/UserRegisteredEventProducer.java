package com.baskaaleksander.nuvine.infrastrucure.messaging;

import com.baskaaleksander.nuvine.application.util.MaskingUtil;
import com.baskaaleksander.nuvine.infrastrucure.messaging.dto.UserRegisteredEvent;
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
public class UserRegisteredEventProducer {

    @Value("${topics.user-registered-topic}")
    private String userRegisteredTopic;

    private final KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

    private void sendPasswordResetEvent(UserRegisteredEvent event) {
        Message<UserRegisteredEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, userRegisteredTopic)
                .build();

        kafkaTemplate.send(message);

        log.info("User registered event sent: {}", MaskingUtil.maskEmail(event.email()));
    }
}
