package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.domain.model.Notification;
import com.baskaaleksander.nuvine.domain.model.NotificationType;
import com.baskaaleksander.nuvine.infrastructure.crypto.CryptoService;
import com.baskaaleksander.nuvine.infrastructure.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CryptoService crypto;

    public void createNotification(
            String userId,
            NotificationType type,
            String payload
    ) {
        String encryptedPayload = crypto.encrypt(payload);
        String payloadHash = crypto.hash(payload);

        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .encryptedPayload(encryptedPayload)
                .payloadHash(payloadHash)
                .createdAt(Instant.now())
                .build();

        notificationRepository.save(notification);
    }
}
