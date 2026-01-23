package com.baskaaleksander.nuvine.integration.repository;

import com.baskaaleksander.nuvine.domain.model.FailedNotification;
import com.baskaaleksander.nuvine.domain.model.Notification;
import com.baskaaleksander.nuvine.domain.model.NotificationType;
import com.baskaaleksander.nuvine.infrastructure.repository.FailedNotificationRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.NotificationRepository;
import com.baskaaleksander.nuvine.integration.base.BaseIntegrationTest;
import com.baskaaleksander.nuvine.integration.support.TestDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private FailedNotificationRepository failedNotificationRepository;

    @Autowired
    private TestDataBuilder testDataBuilder;

    @AfterEach
    void cleanUp() {
        testDataBuilder.cleanUp();
    }

    @Test
    void save_validNotification_persistsToMongoDB() {
        // given
        String userId = UUID.randomUUID().toString();
        Notification notification = Notification.builder()
                .userId(userId)
                .type(NotificationType.EMAIL_VERIFICATION)
                .encryptedPayload("encrypted-payload-data")
                .payloadHash("payload-hash-123")
                .createdAt(Instant.now())
                .build();

        // when
        Notification saved = notificationRepository.save(notification);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getType()).isEqualTo(NotificationType.EMAIL_VERIFICATION);
        assertThat(saved.getEncryptedPayload()).isEqualTo("encrypted-payload-data");
        assertThat(saved.getPayloadHash()).isEqualTo("payload-hash-123");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void findById_existingNotification_returnsNotification() {
        // given
        String userId = UUID.randomUUID().toString();
        Notification notification = testDataBuilder.createNotification(
                userId,
                NotificationType.PASSWORD_RESET,
                "encrypted-payload",
                "hash-123"
        );

        // when
        Optional<Notification> found = notificationRepository.findById(notification.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(userId);
        assertThat(found.get().getType()).isEqualTo(NotificationType.PASSWORD_RESET);
    }

    @Test
    void findById_nonExisting_returnsEmpty() {
        // when
        Optional<Notification> found = notificationRepository.findById("non-existing-id");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    void deleteAll_clearsCollection() {
        // given
        testDataBuilder.createNotification(
                UUID.randomUUID().toString(),
                NotificationType.USER_REGISTERED,
                "encrypted-1",
                "hash-1"
        );
        testDataBuilder.createNotification(
                UUID.randomUUID().toString(),
                NotificationType.WORKSPACE_MEMBER_ADDED,
                "encrypted-2",
                "hash-2"
        );
        assertThat(notificationRepository.count()).isEqualTo(2);

        // when
        notificationRepository.deleteAll();

        // then
        assertThat(notificationRepository.count()).isZero();
    }

    @Test
    void save_failedNotification_persistsToMongoDB() {
        // given
        String userId = UUID.randomUUID().toString();
        FailedNotification failedNotification = FailedNotification.builder()
                .userId(userId)
                .type(NotificationType.EMAIL_VERIFICATION)
                .encryptedPayload("encrypted-payload-data")
                .payloadHash("payload-hash-123")
                .originalTopic("email-verification-topic")
                .originalPartition("0")
                .originalOffset(100L)
                .exceptionMessage("Connection refused")
                .exceptionClass("jakarta.mail.MessagingException")
                .failedAt(Instant.now())
                .replayed(false)
                .build();

        // when
        FailedNotification saved = failedNotificationRepository.save(failedNotification);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getType()).isEqualTo(NotificationType.EMAIL_VERIFICATION);
        assertThat(saved.getOriginalTopic()).isEqualTo("email-verification-topic");
        assertThat(saved.getExceptionMessage()).isEqualTo("Connection refused");
        assertThat(saved.getReplayed()).isFalse();
    }

    @Test
    void findById_existingFailedNotification_returnsFailedNotification() {
        // given
        String userId = UUID.randomUUID().toString();
        FailedNotification failedNotification = testDataBuilder.createFailedNotification(
                userId,
                NotificationType.PASSWORD_RESET,
                "encrypted-payload",
                "hash-123",
                "password-reset-topic",
                "0",
                50L,
                "SMTP error",
                "jakarta.mail.MessagingException"
        );

        // when
        Optional<FailedNotification> found = failedNotificationRepository.findById(failedNotification.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(userId);
        assertThat(found.get().getType()).isEqualTo(NotificationType.PASSWORD_RESET);
        assertThat(found.get().getOriginalTopic()).isEqualTo("password-reset-topic");
    }
}
