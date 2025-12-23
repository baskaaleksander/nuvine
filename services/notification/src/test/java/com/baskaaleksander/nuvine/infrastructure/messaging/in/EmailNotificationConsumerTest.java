package com.baskaaleksander.nuvine.infrastructure.messaging.in;

import com.baskaaleksander.nuvine.application.dto.CreateNotificationRequest;
import com.baskaaleksander.nuvine.domain.model.NotificationType;
import com.baskaaleksander.nuvine.domain.service.NotificationEntityService;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.*;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.EmailSender;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailNotificationConsumerTest {

    @Mock
    private EmailSender emailSender;

    @Mock
    private NotificationEntityService notificationEntityService;

    @InjectMocks
    private EmailNotificationConsumer consumer;

    private static final String FRONTEND_URL = "http://frontend";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(consumer, "frontendUrl", FRONTEND_URL);
    }

    @Test
    void onUserRegistered_validEvent_sendsWelcomeEmail() throws Exception {
        UserRegisteredEvent event = new UserRegisteredEvent("John", "Doe", "user@example.com", "token-1", "user-1");

        consumer.onUserRegistered(event);

        verify(emailSender).sendWelcomeEmail(
                "user@example.com",
                "John",
                "Doe",
                FRONTEND_URL + "/verify-email?token=token-1"
        );
    }

    @Test
    void onUserRegistered_validEvent_persistsNotification() throws Exception {
        UserRegisteredEvent event = new UserRegisteredEvent("John", "Doe", "user@example.com", "token-1", "user-1");

        consumer.onUserRegistered(event);

        ArgumentCaptor<CreateNotificationRequest> captor = ArgumentCaptor.forClass(CreateNotificationRequest.class);
        verify(notificationEntityService).createNotification(captor.capture());

        CreateNotificationRequest request = captor.getValue();
        assertEquals("user-1", request.userId());
        assertEquals(NotificationType.USER_REGISTERED, request.type());
        assertEquals(event.toString(), request.payload());
    }

    @Test
    void onPasswordReset_validEvent_sendsResetEmail() throws Exception {
        PasswordResetEvent event = new PasswordResetEvent("user@example.com", "token-1", "user-1");

        consumer.onPasswordReset(event);

        verify(emailSender).sendPasswordResetEmail(
                "user@example.com",
                FRONTEND_URL + "/reset-password?token=token-1"
        );
        verify(notificationEntityService).createNotification(any(CreateNotificationRequest.class));
    }

    @Test
    void onEmailVerification_validEvent_sendsVerificationEmail() throws Exception {
        EmailVerificationEvent event = new EmailVerificationEvent("user@example.com", "token-1", "user-1");

        consumer.onEmailVerification(event);

        verify(emailSender).sendEmailVerificationEmail(
                "user@example.com",
                FRONTEND_URL + "/verify-email?token=token-1"
        );
        verify(notificationEntityService).createNotification(any(CreateNotificationRequest.class));
    }

    @Test
    void onWorkspaceMemberAdded_validEvent_sendsMemberAddedEmail() throws Exception {
        WorkspaceMemberAddedEvent event = new WorkspaceMemberAddedEvent("user@example.com", "user-1", "ws-1", "VIEWER");

        consumer.onWorkspaceMemberAdded(event);

        verify(emailSender).sendMemberAddedEmail(
                "user@example.com",
                "VIEWER",
                FRONTEND_URL + "/workspaces/ws-1"
        );
        verify(notificationEntityService).createNotification(any(CreateNotificationRequest.class));
    }

    @Test
    void onWorkspaceMemberInvited_validEvent_sendsMemberInvitedEmail() throws Exception {
        WorkspaceMemberInvitedEvent event = new WorkspaceMemberInvitedEvent("user@example.com", "ws-1", "Workspace", "VIEWER", "token-1");

        consumer.onWorkspaceMemberInvited(event);

        verify(emailSender).sendMemberInvitedEmail(
                "user@example.com",
                "Workspace",
                "VIEWER",
                FRONTEND_URL + "/ws/invite?token=token-1"
        );
        verify(notificationEntityService).createNotification(any(CreateNotificationRequest.class));
    }

    @Test
    void onPaymentActionRequired_validEvent_sendsPaymentEmailAndPersistsNotification() throws Exception {
        PaymentActionRequiredEvent event = new PaymentActionRequiredEvent("owner@example.com", "inv-1", "http://invoice", "ws-1", "Workspace", "owner-id");

        consumer.onPaymentActionRequired(event);

        verify(emailSender).sendPaymentActionRequiredEmail("owner@example.com", "inv-1", "http://invoice", "ws-1", "Workspace");

        ArgumentCaptor<CreateNotificationRequest> captor = ArgumentCaptor.forClass(CreateNotificationRequest.class);
        verify(notificationEntityService).createNotification(captor.capture());

        CreateNotificationRequest request = captor.getValue();
        assertEquals("owner@example.com", request.userId());
        assertEquals(NotificationType.PAYMENT_ACTION_REQUIRED, request.type());
        assertEquals(event.toString(), request.payload());
    }

    @Test
    void onUserRegistered_emailSenderThrows_propagatesException() throws Exception {
        UserRegisteredEvent event = new UserRegisteredEvent("John", "Doe", "user@example.com", "token-1", "user-1");
        doThrow(new MessagingException("fail"))
                .when(emailSender)
                .sendWelcomeEmail(anyString(), anyString(), anyString(), anyString());

        assertThrows(MessagingException.class, () -> consumer.onUserRegistered(event));

        verify(notificationEntityService, never()).createNotification(any());
    }
}
