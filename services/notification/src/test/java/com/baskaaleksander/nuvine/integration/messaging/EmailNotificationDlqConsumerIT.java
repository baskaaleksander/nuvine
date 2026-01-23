package com.baskaaleksander.nuvine.integration.messaging;

import com.baskaaleksander.nuvine.domain.model.FailedNotification;
import com.baskaaleksander.nuvine.domain.model.NotificationType;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.*;
import com.baskaaleksander.nuvine.infrastructure.repository.FailedNotificationRepository;
import com.baskaaleksander.nuvine.integration.base.BaseKafkaIntegrationTest;
import com.baskaaleksander.nuvine.integration.config.TestEmailSenderConfig;
import com.baskaaleksander.nuvine.integration.support.TestDataBuilder;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.support.KafkaHeaders;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Import(TestEmailSenderConfig.class)
class EmailNotificationDlqConsumerIT extends BaseKafkaIntegrationTest {

    @Autowired
    private FailedNotificationRepository failedNotificationRepository;

    @Autowired
    private TestDataBuilder testDataBuilder;

    @Value("${topics.email-verification-topic}")
    private String emailVerificationTopic;

    @Value("${topics.password-reset-topic}")
    private String passwordResetTopic;

    @Value("${topics.user-registered-topic}")
    private String userRegisteredTopic;

    @Value("${topics.workspace-member-invited-topic}")
    private String workspaceMemberInvitedTopic;

    @BeforeEach
    void setUp() {
        testDataBuilder.cleanUp();
    }

    @AfterEach
    void cleanUp() {
        testDataBuilder.cleanUp();
    }

    @Test
    void handleDlq_emailVerificationEvent_createsFailedNotification() {
        String userId = UUID.randomUUID().toString();
        String email = "failed@example.com";
        String token = "verification-token";
        EmailVerificationEvent event = new EmailVerificationEvent(email, token, userId);
        String dlqTopic = emailVerificationTopic + ".DLT";

        ProducerRecord<String, Object> record = createDlqRecord(
                dlqTopic, userId, event,
                emailVerificationTopic,
                "Connection refused",
                "jakarta.mail.MessagingException"
        );

        kafkaTemplate.send(record);
        kafkaTemplate.flush();

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            FailedNotification failedNotification = failedNotificationRepository.findAll().stream()
                    .filter(n -> n.getType() == NotificationType.EMAIL_VERIFICATION)
                    .filter(n -> n.getUserId().equals(userId))
                    .findFirst()
                    .orElse(null);

            assertThat(failedNotification).isNotNull();
            assertThat(failedNotification.getOriginalTopic()).isEqualTo(emailVerificationTopic);
            assertThat(failedNotification.getExceptionMessage()).isEqualTo("Connection refused");
            assertThat(failedNotification.getExceptionClass()).isEqualTo("jakarta.mail.MessagingException");
            assertThat(failedNotification.getEncryptedPayload()).isNotBlank();
        });
    }

    @Test
    void handleDlq_passwordResetEvent_createsFailedNotification() {
        String userId = UUID.randomUUID().toString();
        String email = "reset-failed@example.com";
        String token = "reset-token";
        PasswordResetEvent event = new PasswordResetEvent(email, token, userId);
        String dlqTopic = passwordResetTopic + ".DLT";

        ProducerRecord<String, Object> record = createDlqRecord(
                dlqTopic, userId, event,
                passwordResetTopic,
                "SMTP timeout",
                "org.springframework.mail.MailSendException"
        );

        kafkaTemplate.send(record);
        kafkaTemplate.flush();

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            FailedNotification failedNotification = failedNotificationRepository.findAll().stream()
                    .filter(n -> n.getType() == NotificationType.PASSWORD_RESET)
                    .filter(n -> n.getUserId().equals(userId))
                    .findFirst()
                    .orElse(null);

            assertThat(failedNotification).isNotNull();
            assertThat(failedNotification.getOriginalTopic()).isEqualTo(passwordResetTopic);
            assertThat(failedNotification.getExceptionMessage()).isEqualTo("SMTP timeout");
        });
    }

    @Test
    void handleDlq_userRegisteredEvent_createsFailedNotification() {
        String userId = UUID.randomUUID().toString();
        String email = "register-failed@example.com";
        String firstName = "Jane";
        String lastName = "Smith";
        String verificationToken = "verification-token";
        UserRegisteredEvent event = new UserRegisteredEvent(firstName, lastName, email, verificationToken, userId);
        String dlqTopic = userRegisteredTopic + ".DLT";

        ProducerRecord<String, Object> record = createDlqRecord(
                dlqTopic, userId, event,
                userRegisteredTopic,
                "Template rendering failed",
                "org.thymeleaf.exceptions.TemplateProcessingException"
        );

        kafkaTemplate.send(record);
        kafkaTemplate.flush();

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            FailedNotification failedNotification = failedNotificationRepository.findAll().stream()
                    .filter(n -> n.getType() == NotificationType.USER_REGISTERED)
                    .filter(n -> n.getUserId().equals(userId))
                    .findFirst()
                    .orElse(null);

            assertThat(failedNotification).isNotNull();
            assertThat(failedNotification.getOriginalTopic()).isEqualTo(userRegisteredTopic);
            assertThat(failedNotification.getExceptionMessage()).isEqualTo("Template rendering failed");
        });
    }

    @Test
    void handleDlq_workspaceMemberInvitedEvent_createsFailedNotification() {
        String email = "invite-failed-" + UUID.randomUUID() + "@example.com";
        String workspaceId = UUID.randomUUID().toString();
        String workspaceName = "Failed Workspace";
        String role = "MEMBER";
        String token = "invite-token";
        WorkspaceMemberInvitedEvent event = new WorkspaceMemberInvitedEvent(
                email, workspaceId, workspaceName, role, token
        );
        String dlqTopic = workspaceMemberInvitedTopic + ".DLT";

        ProducerRecord<String, Object> record = createDlqRecord(
                dlqTopic, email, event,
                workspaceMemberInvitedTopic,
                "Invalid email address",
                "jakarta.mail.internet.AddressException"
        );

        kafkaTemplate.send(record);
        kafkaTemplate.flush();

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            FailedNotification failedNotification = failedNotificationRepository.findAll().stream()
                    .filter(n -> n.getType() == NotificationType.WORKSPACE_MEMBER_INVITED)
                    .filter(n -> n.getUserId().equals(email))
                    .findFirst()
                    .orElse(null);

            assertThat(failedNotification).isNotNull();
            assertThat(failedNotification.getOriginalTopic()).isEqualTo(workspaceMemberInvitedTopic);
            assertThat(failedNotification.getExceptionMessage()).isEqualTo("Invalid email address");
            assertThat(failedNotification.getExceptionClass()).isEqualTo("jakarta.mail.internet.AddressException");
        });
    }

    private ProducerRecord<String, Object> createDlqRecord(
            String dlqTopic,
            String key,
            Object event,
            String originalTopic,
            String exceptionMessage,
            String exceptionClass
    ) {
        ProducerRecord<String, Object> record = new ProducerRecord<>(dlqTopic, key, event);

        record.headers().add(new RecordHeader(
                KafkaHeaders.DLT_ORIGINAL_TOPIC,
                originalTopic.getBytes(StandardCharsets.UTF_8)
        ));
        record.headers().add(new RecordHeader(
                KafkaHeaders.DLT_EXCEPTION_MESSAGE,
                exceptionMessage.getBytes(StandardCharsets.UTF_8)
        ));
        record.headers().add(new RecordHeader(
                KafkaHeaders.DLT_EXCEPTION_FQCN,
                exceptionClass.getBytes(StandardCharsets.UTF_8)
        ));

        return record;
    }
}
