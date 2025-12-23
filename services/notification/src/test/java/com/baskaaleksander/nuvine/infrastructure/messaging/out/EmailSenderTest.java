package com.baskaaleksander.nuvine.infrastructure.messaging.out;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailSenderTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private SpringTemplateEngine templateEngine;

    @InjectMocks
    private EmailSender emailSender;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        mimeMessage = new MimeMessage((Session) null);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        ReflectionTestUtils.setField(emailSender, "senderEmail", "noreply@nuvine.org");
    }

    @Test
    void sendWelcomeEmail_validInput_callsTemplateEngineAndSends() throws Exception {
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html></html>");

        emailSender.sendWelcomeEmail("user@example.com", "John", "Doe", "http://frontend/verify-email?token=1");

        verify(templateEngine).process(anyString(), any(Context.class));
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void sendPasswordResetEmail_validInput_callsTemplateEngineAndSends() throws Exception {
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html></html>");

        emailSender.sendPasswordResetEmail("user@example.com", "http://frontend/reset-password?token=1");

        verify(templateEngine).process(anyString(), any(Context.class));
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void sendEmailVerificationEmail_validInput_callsTemplateEngineAndSends() throws Exception {
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html></html>");

        emailSender.sendEmailVerificationEmail("user@example.com", "http://frontend/verify-email?token=1");

        verify(templateEngine).process(anyString(), any(Context.class));
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void sendMemberAddedEmail_validInput_callsTemplateEngineAndSends() throws Exception {
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html></html>");

        emailSender.sendMemberAddedEmail("user@example.com", "VIEWER", "http://frontend/workspaces/ws-1");

        verify(templateEngine).process(anyString(), any(Context.class));
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void sendMemberInvitedEmail_validInput_callsTemplateEngineAndSends() throws Exception {
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html></html>");

        emailSender.sendMemberInvitedEmail("user@example.com", "Workspace", "VIEWER", "http://frontend/ws/invite?token=1");

        verify(templateEngine).process(anyString(), any(Context.class));
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void sendPaymentActionRequiredEmail_validInput_callsTemplateEngineAndSends() throws Exception {
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html></html>");

        emailSender.sendPaymentActionRequiredEmail("user@example.com", "inv-1", "http://invoice", "ws-1", "Workspace");

        verify(templateEngine).process(anyString(), any(Context.class));
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void sendWelcomeEmail_templateEngineThrows_propagatesExceptionAndDoesNotSend() {
        when(templateEngine.process(anyString(), any(Context.class))).thenThrow(new RuntimeException("fail"));

        assertThrows(
                RuntimeException.class,
                () -> emailSender.sendWelcomeEmail("user@example.com", "John", "Doe", "http://frontend/verify-email?token=1")
        );

        verify(javaMailSender, never()).send(any(MimeMessage.class));
    }
}
