package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.CreateFailedNotificationRequest;
import com.baskaaleksander.nuvine.application.dto.CreateNotificationRequest;
import com.baskaaleksander.nuvine.domain.model.FailedNotification;
import com.baskaaleksander.nuvine.domain.model.Notification;
import com.baskaaleksander.nuvine.domain.model.NotificationType;
import com.baskaaleksander.nuvine.infrastructure.crypto.CryptoService;
import com.baskaaleksander.nuvine.infrastructure.repository.FailedNotificationRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEntityService {

    private final NotificationRepository notificationRepository;
    private final FailedNotificationRepository failedNotificationRepository;
    private final CryptoService crypto;

    public void createNotification(
        CreateNotificationRequest request
    ) {
        String encryptedPayload = crypto.encrypt(request.payload());
        String payloadHash = crypto.hash(request.payload());

        Notification notification = Notification.builder()
                .userId(request.userId())
                .type(request.type())
                .encryptedPayload(encryptedPayload)
                .payloadHash(payloadHash)
                .createdAt(Instant.now())
                .build();

        notificationRepository.save(notification);
        log.info("Notification created id={} userId={}", notification.getId(), request.userId());
    }

    public void saveFailedFromDlq(
        CreateFailedNotificationRequest request
    ) {
        String encryptedPayload = crypto.encrypt(request.payload());
        String payloadHash = crypto.hash(request.payload());

        FailedNotification failedNotification = FailedNotification.builder()
                .userId(request.userId())
                .type(request.type())
                .encryptedPayload(encryptedPayload)
                .payloadHash(payloadHash)
                .originalTopic(request.originalTopic())
                .originalPartition(request.originalPartition())
                .originalOffset(request.originalOffset())
                .exceptionMessage(request.exceptionMessage())
                .exceptionClass(request.exceptionClass())
                .failedAt(Instant.now())
                .build();

        failedNotificationRepository.save(failedNotification);
        log.info("Failed notification saved id={} userId={}", failedNotification.getId(), request.userId());
    }
}
