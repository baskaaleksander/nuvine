package com.baskaaleksander.nuvine.integration.support;

import com.baskaaleksander.nuvine.domain.model.FailedNotification;
import com.baskaaleksander.nuvine.domain.model.Notification;
import com.baskaaleksander.nuvine.domain.model.NotificationType;
import com.baskaaleksander.nuvine.infrastructure.repository.FailedNotificationRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.NotificationRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class TestDataBuilder {

    private final NotificationRepository notificationRepository;
    private final FailedNotificationRepository failedNotificationRepository;

    public TestDataBuilder(NotificationRepository notificationRepository,
                           FailedNotificationRepository failedNotificationRepository) {
        this.notificationRepository = notificationRepository;
        this.failedNotificationRepository = failedNotificationRepository;
    }

    public Notification createNotification(String userId, NotificationType type,
                                           String encryptedPayload, String payloadHash) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .encryptedPayload(encryptedPayload)
                .payloadHash(payloadHash)
                .createdAt(Instant.now())
                .build();
        return notificationRepository.save(notification);
    }

    public FailedNotification createFailedNotification(String userId, NotificationType type,
                                                       String encryptedPayload, String payloadHash,
                                                       String originalTopic, String originalPartition,
                                                       Long originalOffset, String exceptionMessage,
                                                       String exceptionClass) {
        FailedNotification failedNotification = FailedNotification.builder()
                .userId(userId)
                .type(type)
                .encryptedPayload(encryptedPayload)
                .payloadHash(payloadHash)
                .originalTopic(originalTopic)
                .originalPartition(originalPartition)
                .originalOffset(originalOffset)
                .exceptionMessage(exceptionMessage)
                .exceptionClass(exceptionClass)
                .failedAt(Instant.now())
                .replayed(false)
                .build();
        return failedNotificationRepository.save(failedNotification);
    }

    public void cleanUp() {
        failedNotificationRepository.deleteAll();
        notificationRepository.deleteAll();
    }
}
