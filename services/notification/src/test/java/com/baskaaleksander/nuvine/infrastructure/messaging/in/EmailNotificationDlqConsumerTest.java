package com.baskaaleksander.nuvine.infrastructure.messaging.in;

import com.baskaaleksander.nuvine.application.dto.CreateFailedNotificationRequest;
import com.baskaaleksander.nuvine.domain.model.NotificationType;
import com.baskaaleksander.nuvine.domain.service.NotificationEntityService;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmailVerificationEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.PasswordResetEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.UserRegisteredEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.WorkspaceMemberInvitedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailNotificationDlqConsumerTest {

    @Mock
    private NotificationEntityService notificationEntityService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EmailNotificationDlqConsumer consumer;

    @Test
    void handleDlq_emailVerificationEvent_persistsFailedNotification() throws Exception {
        EmailVerificationEvent event = new EmailVerificationEvent("user@example.com", "token-1", "user-1");
        ConsumerRecord<String, Object> record = new ConsumerRecord<>("topic.DLT", 2, 100L, null, event);
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"k\":\"v\"}");

        consumer.handleDlq(record, "original-topic", "boom", "java.lang.RuntimeException");

        ArgumentCaptor<CreateFailedNotificationRequest> captor = ArgumentCaptor.forClass(CreateFailedNotificationRequest.class);
        verify(notificationEntityService).saveFailedFromDlq(captor.capture());

        CreateFailedNotificationRequest request = captor.getValue();
        assertEquals("user-1", request.userId());
        assertEquals(NotificationType.EMAIL_VERIFICATION, request.type());
        assertEquals("{\"k\":\"v\"}", request.payload());
        assertEquals("original-topic", request.originalTopic());
        assertEquals("2", request.originalPartition());
        assertEquals(100L, request.originalOffset());
        assertEquals("boom", request.exceptionMessage());
        assertEquals("java.lang.RuntimeException", request.exceptionClass());
    }

    @Test
    void handleDlq_passwordResetEvent_persistsFailedNotification() throws Exception {
        PasswordResetEvent event = new PasswordResetEvent("user@example.com", "token-1", "user-1");
        ConsumerRecord<String, Object> record = new ConsumerRecord<>("topic.DLT", 1, 10L, null, event);
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"event\":\"reset\"}");

        consumer.handleDlq(record, "password-reset-topic", "boom", "java.lang.RuntimeException");

        ArgumentCaptor<CreateFailedNotificationRequest> captor = ArgumentCaptor.forClass(CreateFailedNotificationRequest.class);
        verify(notificationEntityService).saveFailedFromDlq(captor.capture());
        assertEquals(NotificationType.PASSWORD_RESET, captor.getValue().type());
    }

    @Test
    void handleDlq_userRegisteredEvent_persistsFailedNotification() throws Exception {
        UserRegisteredEvent event = new UserRegisteredEvent("John", "Doe", "user@example.com", "token-1", "user-1");
        ConsumerRecord<String, Object> record = new ConsumerRecord<>("topic.DLT", 0, 1L, null, event);
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"event\":\"registered\"}");

        consumer.handleDlq(record, "user-registered-topic", "boom", "java.lang.RuntimeException");

        ArgumentCaptor<CreateFailedNotificationRequest> captor = ArgumentCaptor.forClass(CreateFailedNotificationRequest.class);
        verify(notificationEntityService).saveFailedFromDlq(captor.capture());
        assertEquals(NotificationType.USER_REGISTERED, captor.getValue().type());
    }

    @Test
    void handleDlq_workspaceMemberInvitedEvent_persistsFailedNotification() throws Exception {
        WorkspaceMemberInvitedEvent event = new WorkspaceMemberInvitedEvent("user@example.com", "ws-1", "Workspace", "VIEWER", "token-1");
        ConsumerRecord<String, Object> record = new ConsumerRecord<>("topic.DLT", 0, 1L, null, event);
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"event\":\"invited\"}");

        consumer.handleDlq(record, "workspace-member-invited-topic", "boom", "java.lang.RuntimeException");

        ArgumentCaptor<CreateFailedNotificationRequest> captor = ArgumentCaptor.forClass(CreateFailedNotificationRequest.class);
        verify(notificationEntityService).saveFailedFromDlq(captor.capture());
        assertEquals(NotificationType.WORKSPACE_MEMBER_INVITED, captor.getValue().type());
        assertEquals("user@example.com", captor.getValue().userId());
    }

    @Test
    void handleDlq_unknownPayload_stillPersistsWithNullTypeAndUserId() throws Exception {
        Object payload = new Object();
        ConsumerRecord<String, Object> record = new ConsumerRecord<>("topic.DLT", 0, 1L, null, payload);
        when(objectMapper.writeValueAsString(payload)).thenReturn("{}" );

        consumer.handleDlq(record, "unknown-topic", "boom", "java.lang.RuntimeException");

        ArgumentCaptor<CreateFailedNotificationRequest> captor = ArgumentCaptor.forClass(CreateFailedNotificationRequest.class);
        verify(notificationEntityService).saveFailedFromDlq(captor.capture());
        assertEquals(null, captor.getValue().userId());
        assertEquals(null, captor.getValue().type());
    }

    @Test
    void handleDlq_objectMapperFails_doesNotThrow() throws Exception {
        EmailVerificationEvent event = new EmailVerificationEvent("user@example.com", "token-1", "user-1");
        ConsumerRecord<String, Object> record = new ConsumerRecord<>("topic.DLT", 0, 1L, null, event);
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("boom"));

        assertDoesNotThrow(() -> consumer.handleDlq(record, "original-topic", "boom", "java.lang.RuntimeException"));

        verify(notificationEntityService, never()).saveFailedFromDlq(any());
    }
}
