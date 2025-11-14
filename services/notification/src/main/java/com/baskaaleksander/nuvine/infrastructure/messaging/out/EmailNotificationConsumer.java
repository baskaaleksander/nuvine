package com.baskaaleksander.nuvine.infrastructure.messaging.out;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmailVerificationEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.PasswordResetEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailNotificationConsumer {

    @KafkaListener(topics = "${topics.email-verification-topic}", groupId = "${spring.kafka.consumer.group-id:notification-service}")
    public void onEmailVerification(EmailVerificationEvent event) {
        log.info("Email verification event received: {}", event.toString());
    }

    @KafkaListener(topics = "${topics.password-reset-topic}", groupId = "${spring.kafka.consumer.group-id:notification-service}")
    public void onPasswordReset(PasswordResetEvent event) {
        log.info("Password reset event received: {}", event.toString());
    }

    @KafkaListener(topics = "${topics.user-registered-topic}", groupId = "${spring.kafka.consumer.group-id:notification-service}")
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("User registered event received: {}", event.toString());
    }
}
