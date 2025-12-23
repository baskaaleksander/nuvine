package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.CreateFailedNotificationRequest;
import com.baskaaleksander.nuvine.application.dto.CreateNotificationRequest;
import com.baskaaleksander.nuvine.domain.model.FailedNotification;
import com.baskaaleksander.nuvine.domain.model.Notification;
import com.baskaaleksander.nuvine.domain.model.NotificationType;
import com.baskaaleksander.nuvine.infrastructure.crypto.CryptoService;
import com.baskaaleksander.nuvine.infrastructure.repository.FailedNotificationRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEntityServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private FailedNotificationRepository failedNotificationRepository;

    @Mock
    private CryptoService cryptoService;

    @InjectMocks
    private NotificationEntityService notificationEntityService;

    private String userId;
    private String payload;

    @BeforeEach
    void setUp() {
        userId = "user-1";
        payload = "payload";
    }

    @Test
    void createNotification_validRequest_encryptsPayload() {
        when(cryptoService.encrypt(payload)).thenReturn("encrypted");
        when(cryptoService.hash(payload)).thenReturn("hash");

        notificationEntityService.createNotification(
                new CreateNotificationRequest(userId, NotificationType.USER_REGISTERED, payload)
        );

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals("encrypted", saved.getEncryptedPayload());
        assertEquals("hash", saved.getPayloadHash());
        assertEquals(userId, saved.getUserId());
        assertEquals(NotificationType.USER_REGISTERED, saved.getType());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void createNotification_validRequest_persistsToRepository() {
        when(cryptoService.encrypt(payload)).thenReturn("encrypted");
        when(cryptoService.hash(payload)).thenReturn("hash");

        notificationEntityService.createNotification(
                new CreateNotificationRequest(userId, NotificationType.EMAIL_VERIFICATION, payload)
        );

        verify(notificationRepository).save(org.mockito.ArgumentMatchers.any(Notification.class));
    }

    @Test
    void saveFailedFromDlq_validRequest_persistsFailedNotification() {
        when(cryptoService.encrypt(payload)).thenReturn("encrypted");
        when(cryptoService.hash(payload)).thenReturn("hash");

        CreateFailedNotificationRequest request = new CreateFailedNotificationRequest(
                userId,
                NotificationType.PASSWORD_RESET,
                payload,
                "original-topic",
                "2",
                100L,
                "boom",
                "java.lang.RuntimeException"
        );

        notificationEntityService.saveFailedFromDlq(request);

        ArgumentCaptor<FailedNotification> captor = ArgumentCaptor.forClass(FailedNotification.class);
        verify(failedNotificationRepository).save(captor.capture());

        FailedNotification saved = captor.getValue();
        assertEquals(userId, saved.getUserId());
        assertEquals(NotificationType.PASSWORD_RESET, saved.getType());
        assertEquals("encrypted", saved.getEncryptedPayload());
        assertEquals("hash", saved.getPayloadHash());
        assertEquals("original-topic", saved.getOriginalTopic());
        assertEquals("2", saved.getOriginalPartition());
        assertEquals(100L, saved.getOriginalOffset());
        assertEquals("boom", saved.getExceptionMessage());
        assertEquals("java.lang.RuntimeException", saved.getExceptionClass());
        assertNotNull(saved.getFailedAt());
    }
}
