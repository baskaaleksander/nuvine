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
import org.springframework.scheduling.annotation.Async;
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
            log.info("Welcome email sent email={}", MaskingUtil.maskEmail(to));
        } catch (Exception e) {
            log.error("Failed to send welcome email to email={}", MaskingUtil.maskEmail(to), e);
            throw e;
        }
    }

    public void sendPasswordResetEmail(String to, String passwordResetUrl) throws MessagingException {
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
            log.info("Password reset email sent email={}", MaskingUtil.maskEmail(to));
        } catch (Exception e) {
            log.error("Failed to send password reset email to email={}", MaskingUtil.maskEmail(to), e);
            throw e;
        }
    }

    public void sendEmailVerificationEmail(String to, String emailVerificationUrl) throws MessagingException {
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
            log.info("Email verification email sent email={}", MaskingUtil.maskEmail(to));
        } catch (Exception e) {
            log.error("Failed to send email verification email to email={}", MaskingUtil.maskEmail(to), e);
            throw e;
        }
    }
}
