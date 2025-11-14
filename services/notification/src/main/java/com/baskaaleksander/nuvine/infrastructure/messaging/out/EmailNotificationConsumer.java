package com.baskaaleksander.nuvine.infrastructure.messaging.out;

import com.baskaaleksander.nuvine.application.dto.CreateNotificationRequest;
import com.baskaaleksander.nuvine.application.util.MaskingUtil;
import com.baskaaleksander.nuvine.domain.model.NotificationType;
import com.baskaaleksander.nuvine.domain.service.NotificationEntityService;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmailVerificationEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.PasswordResetEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.UserRegisteredEvent;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailNotificationConsumer {

    private final EmailSender emailSender;
    private final NotificationEntityService service;
    @Value("${application.frontend-url}")
    private String frontendUrl;

    @KafkaListener(topics = "${topics.email-verification-topic}", groupId = "${spring.kafka.consumer.group-id:notification-service}")
    public void onEmailVerification(EmailVerificationEvent event) throws MessagingException {
        log.info("Email verification event received: {}", event.toString());
        try {
            emailSender.sendEmailVerificationEmail(event.email(), frontendUrl + "/verify-email?token=" + event.token());
            service.createNotification(
                    new CreateNotificationRequest(
                            event.userId(),
                            NotificationType.EMAIL_VERIFICATION,
                            event.toString()
                    ));
        } catch (Exception e) {
            log.error("Failed to send email verification email to email={}", MaskingUtil.maskEmail(event.email()), e);
            throw e;
        }
    }

    @KafkaListener(topics = "${topics.password-reset-topic}", groupId = "${spring.kafka.consumer.group-id:notification-service}")
    public void onPasswordReset(PasswordResetEvent event) throws MessagingException {
        log.info("Password reset event received: {}", event.toString());
        try {
            emailSender.sendPasswordResetEmail(event.email(), frontendUrl + "/reset-password?token=" + event.token());
            service.createNotification(
                    new CreateNotificationRequest(
                            event.userId(),
                            NotificationType.PASSWORD_RESET,
                            event.toString()
                    ));
        } catch (Exception e) {
            log.error("Failed to send password reset email to email={}", MaskingUtil.maskEmail(event.email()), e);
            throw e;
        }
    }

    @KafkaListener(topics = "${topics.user-registered-topic}", groupId = "${spring.kafka.consumer.group-id:notification-service}")
    public void onUserRegistered(UserRegisteredEvent event) throws MessagingException {
        log.info("User registered event received: {}", event.toString());
        try {
            emailSender.sendWelcomeEmail(event.email(), event.firstName(), event.lastName(), frontendUrl + "/verify-email?token=" + event.emailVerificationToken());
            service.createNotification(
                    new CreateNotificationRequest(
                            event.userId(),
                            NotificationType.USER_REGISTERED,
                            event.toString()
                    ));
        } catch (Exception e) {
            log.error("Failed to send welcome email to email={}", MaskingUtil.maskEmail(event.email()), e);
            throw e;
        }
    }
}
