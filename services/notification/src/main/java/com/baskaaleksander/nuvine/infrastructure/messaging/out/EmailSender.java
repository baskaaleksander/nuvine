package com.baskaaleksander.nuvine.infrastructure.messaging.out;

import com.baskaaleksander.nuvine.application.util.MaskingUtil;
import com.baskaaleksander.nuvine.domain.model.EmailTemplates;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailSender {

    private final JavaMailSender sender;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.email}")
    private String senderEmail;

    public void sendWelcomeEmail(String to, String firstName, String lastName, String emailVerificationUrl) throws MessagingException {
        log.info("SEND_WELCOME_EMAIL START to={}", MaskingUtil.maskEmail(to));
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper messageHelper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
        messageHelper.setFrom("senderEmail");

        final String templateName = EmailTemplates.USER_REGISTERED.getTemplateName();
        final String subject = EmailTemplates.USER_REGISTERED.getSubject();

        Map<String, Object> variables = new HashMap<>();

        variables.put("firstName", firstName);
        variables.put("lastName", lastName);
        variables.put("verifyUrl", emailVerificationUrl);

        Context context = new Context();
        context.setVariables(variables);
        messageHelper.setSubject(subject);

        try {
            String htmlContent = templateEngine.process(templateName, context);
            String plainContent = String.format(
                    """
                            Hi %s %s,
                            
                            Welcome to Nuvine!
                            Please verify your email using the link below:
                            %s
                            
                            If you did not create an account, you can safely ignore this message.
                            
                            Visit us at https://nuvine.org""",
                    firstName,
                    lastName,
                    emailVerificationUrl
            );
            messageHelper.setText(plainContent, htmlContent);

            messageHelper.setTo(to);
            sender.send(message);
            log.info("SEND_WELCOME_EMAIL SUCCESS to={}", MaskingUtil.maskEmail(to));
        } catch (Exception e) {
            log.error("SEND_WELCOME_EMAIL FAILED to={}", MaskingUtil.maskEmail(to), e);
            throw e;
        }
    }

    public void sendPasswordResetEmail(String to, String passwordResetUrl) throws MessagingException {
        log.info("SEND_PASSWORD_RESET_EMAIL START to={}", MaskingUtil.maskEmail(to));
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper messageHelper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
        messageHelper.setFrom(senderEmail);

        final String templateName = EmailTemplates.PASSWORD_RESET.getTemplateName();
        final String subject = EmailTemplates.PASSWORD_RESET.getSubject();

        Map<String, Object> variables = new HashMap<>();

        variables.put("resetUrl", passwordResetUrl);

        Context context = new Context();
        context.setVariables(variables);
        messageHelper.setSubject(subject);

        try {
            String htmlContent = templateEngine.process(templateName, context);
            String plainContent = String.format(
                    """
                            You requested a password reset for your Nuvine account.
                            Reset your password using the link below:
                            %s
                            
                            If you didn't request a password reset, you can safely ignore this email.
                            
                            Visit us at https://nuvine.org""",
                    passwordResetUrl
            );
            messageHelper.setText(plainContent, htmlContent);

            messageHelper.setTo(to);
            sender.send(message);
            log.info("SEND_PASSWORD_RESET_EMAIL SUCCESS to={}", MaskingUtil.maskEmail(to));
        } catch (Exception e) {
            log.error("SEND_PASSWORD_RESET_EMAIL FAILED to={}", MaskingUtil.maskEmail(to), e);
            throw e;
        }
    }

    public void sendEmailVerificationEmail(String to, String emailVerificationUrl) throws MessagingException {
        log.info("SEND_EMAIL_VERIFICATION_EMAIL START to={}", MaskingUtil.maskEmail(to));
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper messageHelper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
        messageHelper.setFrom(senderEmail);

        final String templateName = EmailTemplates.EMAIL_VERIFICATION.getTemplateName();
        final String subject = EmailTemplates.EMAIL_VERIFICATION.getSubject();

        Map<String, Object> variables = new HashMap<>();

        variables.put("verifyUrl", emailVerificationUrl);

        Context context = new Context();
        context.setVariables(variables);
        messageHelper.setSubject(subject);

        try {
            String htmlContent = templateEngine.process(templateName, context);
            String plainContent = String.format(
                    """
                            You requested a email verification for your Nuvine account.
                            Verify your email using the link below:
                            %s
                            
                            If you didn't request a email verification, you can safely ignore this email.
                            
                            Visit us at https://nuvine.org""",
                    emailVerificationUrl
            );
            messageHelper.setText(plainContent, htmlContent);

            messageHelper.setTo(to);
            sender.send(message);
            log.info("SEND_EMAIL_VERIFICATION_URL SUCCESS to={}", MaskingUtil.maskEmail(to));
        } catch (Exception e) {
            log.error("SEND_EMAIL_VERIFICATION_URL FAILED to={}", MaskingUtil.maskEmail(to), e);
            throw e;
        }
    }

    public void sendMemberAddedEmail(String to, String role, String workspaceUrl) throws MessagingException {
        log.info("SEND_MEMBER_ADDED_EMAIL START to={}", MaskingUtil.maskEmail(to));

        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper messageHelper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
        messageHelper.setFrom(senderEmail);

        final String templateName = EmailTemplates.WORKSPACE_MEMBER_ADDED.getTemplateName();
        final String subject = EmailTemplates.WORKSPACE_MEMBER_ADDED.getSubject();

        Map<String, Object> variables = new HashMap<>();
        variables.put("email", to);
        variables.put("role", role);
        variables.put("openWorkspaceUrl", workspaceUrl);

        Context context = new Context();
        context.setVariables(variables);
        messageHelper.setSubject(subject);

        try {
            String htmlContent = templateEngine.process(templateName, context);
            String plainContent = String.format(
                    """
                            You have been added to a workspace on Nuvine.
                            Your assigned role: %s
                            
                            You can open the workspace using the link below:
                            %s
                            
                            If you didn't expect this change, you can safely ignore this email.
                            
                            Visit us at https://nuvine.org
                            """,
                    role,
                    workspaceUrl
            );
            messageHelper.setText(plainContent, htmlContent);

            messageHelper.setTo(to);
            sender.send(message);
            log.info("SEND_MEMBER_ADDED_EMAIL SUCCESS email={}", MaskingUtil.maskEmail(to));
        } catch (Exception e) {
            log.error("SEND_MEMBER_ADDED_EMAIL FAILED to={}", MaskingUtil.maskEmail(to), e);
            throw e;
        }

    }
}
