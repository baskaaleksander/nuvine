package com.baskaaleksander.nuvine.integration.messaging;

import com.baskaaleksander.nuvine.domain.model.Notification;
import com.baskaaleksander.nuvine.domain.model.NotificationType;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.*;
import com.baskaaleksander.nuvine.infrastructure.repository.NotificationRepository;
import com.baskaaleksander.nuvine.integration.base.BaseKafkaIntegrationTest;
import com.baskaaleksander.nuvine.integration.config.TestEmailSenderConfig;
import com.baskaaleksander.nuvine.integration.config.TestEmailSenderConfig.TestEmailSender;
import com.baskaaleksander.nuvine.integration.support.TestDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Import(TestEmailSenderConfig.class)
class EmailNotificationConsumerIT extends BaseKafkaIntegrationTest {

    @Autowired
    private TestEmailSender testEmailSender;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TestDataBuilder testDataBuilder;

    @Value("${topics.email-verification-topic}")
    private String emailVerificationTopic;

    @Value("${topics.password-reset-topic}")
    private String passwordResetTopic;

    @Value("${topics.user-registered-topic}")
    private String userRegisteredTopic;

    @Value("${topics.workspace-member-added-topic}")
    private String workspaceMemberAddedTopic;

    @Value("${topics.workspace-member-invited-topic}")
    private String workspaceMemberInvitedTopic;

    @Value("${topics.payment-action-required-topic}")
    private String paymentActionRequiredTopic;

    @BeforeEach
    void setUp() {
        testDataBuilder.cleanUp();
        testEmailSender.clearCalls();
    }

    @AfterEach
    void cleanUp() {
        testDataBuilder.cleanUp();
    }

    @Test
    void onEmailVerification_validEvent_createsNotificationAndSendsEmail() {
        String userId = UUID.randomUUID().toString();
        String email = "test@example.com";
        String token = "verification-token-123";
        EmailVerificationEvent event = new EmailVerificationEvent(email, token, userId);

        kafkaTemplate.send(emailVerificationTopic, userId, event);
        kafkaTemplate.flush();

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            Notification notification = notificationRepository.findAll().stream()
                    .filter(n -> n.getType() == NotificationType.EMAIL_VERIFICATION)
                    .filter(n -> n.getUserId().equals(userId))
                    .findFirst()
                    .orElse(null);

            assertThat(notification).isNotNull();
            assertThat(notification.getEncryptedPayload()).isNotBlank();
            assertThat(notification.getPayloadHash()).isNotBlank();
            assertThat(testEmailSender.wasCalledWith("verification", email)).isTrue();
        });
    }

    @Test
    void onPasswordReset_validEvent_createsNotificationAndSendsEmail() {
        String userId = UUID.randomUUID().toString();
        String email = "reset@example.com";
        String token = "reset-token-456";
        PasswordResetEvent event = new PasswordResetEvent(email, token, userId);

        kafkaTemplate.send(passwordResetTopic, userId, event);
        kafkaTemplate.flush();

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            Notification notification = notificationRepository.findAll().stream()
                    .filter(n -> n.getType() == NotificationType.PASSWORD_RESET)
                    .filter(n -> n.getUserId().equals(userId))
                    .findFirst()
                    .orElse(null);

            assertThat(notification).isNotNull();
            assertThat(testEmailSender.wasCalledWith("passwordReset", email)).isTrue();
        });
    }

    @Test
    void onUserRegistered_validEvent_createsNotificationAndSendsEmail() {
        String userId = UUID.randomUUID().toString();
        String email = "newuser@example.com";
        String firstName = "John";
        String lastName = "Doe";
        String verificationToken = "welcome-token-789";
        UserRegisteredEvent event = new UserRegisteredEvent(firstName, lastName, email, verificationToken, userId);

        kafkaTemplate.send(userRegisteredTopic, userId, event);
        kafkaTemplate.flush();

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            Notification notification = notificationRepository.findAll().stream()
                    .filter(n -> n.getType() == NotificationType.USER_REGISTERED)
                    .filter(n -> n.getUserId().equals(userId))
                    .findFirst()
                    .orElse(null);

            assertThat(notification).isNotNull();
            assertThat(testEmailSender.wasCalledWith("welcome", email, firstName, lastName)).isTrue();
        });
    }

    @Test
    void onWorkspaceMemberAdded_validEvent_createsNotificationAndSendsEmail() {
        String userId = UUID.randomUUID().toString();
        String email = "member@example.com";
        String workspaceId = UUID.randomUUID().toString();
        String role = "MEMBER";
        WorkspaceMemberAddedEvent event = new WorkspaceMemberAddedEvent(email, userId, workspaceId, role);

        kafkaTemplate.send(workspaceMemberAddedTopic, userId, event);
        kafkaTemplate.flush();

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            Notification notification = notificationRepository.findAll().stream()
                    .filter(n -> n.getType() == NotificationType.WORKSPACE_MEMBER_ADDED)
                    .filter(n -> n.getUserId().equals(userId))
                    .findFirst()
                    .orElse(null);

            assertThat(notification).isNotNull();
            assertThat(testEmailSender.wasCalledWith("memberAdded", email, role)).isTrue();
        });
    }

    @Test
    void onWorkspaceMemberInvited_validEvent_createsNotificationAndSendsEmail() {
        String email = "invited-" + UUID.randomUUID() + "@example.com";
        String workspaceId = UUID.randomUUID().toString();
        String workspaceName = "Test Workspace";
        String role = "VIEWER";
        String token = "invite-token-abc";
        WorkspaceMemberInvitedEvent event = new WorkspaceMemberInvitedEvent(email, workspaceId, workspaceName, role, token);

        kafkaTemplate.send(workspaceMemberInvitedTopic, email, event);
        kafkaTemplate.flush();

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            Notification notification = notificationRepository.findAll().stream()
                    .filter(n -> n.getType() == NotificationType.WORKSPACE_MEMBER_INVITED)
                    .filter(n -> n.getUserId().equals(email))
                    .findFirst()
                    .orElse(null);

            assertThat(notification).isNotNull();
            assertThat(testEmailSender.wasCalledWith("memberInvited", email, workspaceName, role)).isTrue();
        });
    }

    @Test
    void onPaymentActionRequired_validEvent_createsNotificationAndSendsEmail() {
        String ownerEmail = "owner-" + UUID.randomUUID() + "@example.com";
        String ownerId = UUID.randomUUID().toString();
        String invoiceId = "inv_123456";
        String invoiceUrl = "https://stripe.com/invoice/123";
        String workspaceId = UUID.randomUUID().toString();
        String workspaceName = "Premium Workspace";
        PaymentActionRequiredEvent event = new PaymentActionRequiredEvent(
                ownerEmail, invoiceId, invoiceUrl, workspaceId, workspaceName, ownerId
        );

        kafkaTemplate.send(paymentActionRequiredTopic, ownerId, event);
        kafkaTemplate.flush();

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            Notification notification = notificationRepository.findAll().stream()
                    .filter(n -> n.getType() == NotificationType.PAYMENT_ACTION_REQUIRED)
                    .filter(n -> n.getUserId().equals(ownerEmail))
                    .findFirst()
                    .orElse(null);

            assertThat(notification).isNotNull();
            assertThat(testEmailSender.wasCalledWith("paymentActionRequired", ownerEmail, invoiceId, invoiceUrl, workspaceId, workspaceName)).isTrue();
        });
    }
}
