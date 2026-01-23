package com.baskaaleksander.nuvine.integration.config;

import com.baskaaleksander.nuvine.infrastructure.messaging.out.EmailSender;
import jakarta.mail.MessagingException;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@TestConfiguration
public class TestEmailSenderConfig {

    @Bean
    @Primary
    public TestEmailSender testEmailSender() {
        return new TestEmailSender();
    }

    public static class TestEmailSender extends EmailSender {
        private final List<EmailCall> calls = new CopyOnWriteArrayList<>();

        public TestEmailSender() {
            super(null, null);
        }

        @Override
        public void sendWelcomeEmail(String to, String firstName, String lastName, String verificationUrl) throws MessagingException {
            calls.add(new EmailCall("welcome", to, firstName, lastName, verificationUrl));
        }

        @Override
        public void sendEmailVerificationEmail(String to, String verificationUrl) throws MessagingException {
            calls.add(new EmailCall("verification", to, verificationUrl));
        }

        @Override
        public void sendPasswordResetEmail(String to, String resetUrl) throws MessagingException {
            calls.add(new EmailCall("passwordReset", to, resetUrl));
        }

        @Override
        public void sendMemberInvitedEmail(String to, String workspaceName, String role, String inviteUrl) throws MessagingException {
            calls.add(new EmailCall("memberInvited", to, workspaceName, role, inviteUrl));
        }

        @Override
        public void sendMemberAddedEmail(String to, String role, String workspaceUrl) throws MessagingException {
            calls.add(new EmailCall("memberAdded", to, role, workspaceUrl));
        }

        @Override
        public void sendPaymentActionRequiredEmail(String to, String invoiceId, String invoiceUrl, String workspaceId, String workspaceName) throws MessagingException {
            calls.add(new EmailCall("paymentActionRequired", to, invoiceId, invoiceUrl, workspaceId, workspaceName));
        }

        public List<EmailCall> getCalls() {
            return new ArrayList<>(calls);
        }

        public void clearCalls() {
            calls.clear();
        }

        public boolean wasCalledWith(String method, String... expectedArgs) {
            return calls.stream().anyMatch(call ->
                call.method.equals(method) && containsAllArgs(call.args, expectedArgs)
            );
        }

        private boolean containsAllArgs(String[] actual, String[] expected) {
            if (actual.length < expected.length) return false;
            for (int i = 0; i < expected.length; i++) {
                if (expected[i] != null && !expected[i].equals(actual[i])) {
                    return false;
                }
            }
            return true;
        }

        public record EmailCall(String method, String... args) {}
    }
}
