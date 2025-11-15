package com.baskaaleksander.nuvine.infrastructure.messaging;

import com.baskaaleksander.nuvine.application.util.MaskingUtil;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.UserRegisteredEvent;
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

    public void sendUserRegisteredEvent(UserRegisteredEvent event) {
        Message<UserRegisteredEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, userRegisteredTopic)
                .build();

        kafkaTemplate.send(message);

        log.info("SEND_USER_REGISTERED_EVENT SUCCESS email={}", MaskingUtil.maskEmail(event.email()));
    }
}
