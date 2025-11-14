package com.baskaaleksander.nuvine.infrastrucure.messaging;

import com.baskaaleksander.nuvine.application.util.MaskingUtil;
import com.baskaaleksander.nuvine.infrastrucure.messaging.dto.EmailVerificationEvent;
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
public class EmailVerificationEventProducer {

    @Value("${topics.email-verification-topic}")
    private String emailVerificationTopic;

    private final KafkaTemplate<String, EmailVerificationEvent> kafkaTemplate;

    public void sendEmailVerificationEvent(EmailVerificationEvent event) {
        Message<EmailVerificationEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, emailVerificationTopic)
                .build();

        kafkaTemplate.send(message);

        log.info("Email verification event sent: {}", MaskingUtil.maskEmail(event.email()));
    }
}
