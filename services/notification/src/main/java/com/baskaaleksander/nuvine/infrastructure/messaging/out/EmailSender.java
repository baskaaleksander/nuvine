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

    @Value("${spring.mail.email")
    private String senderEmail;

    @Async
    public void sendWelcomeEmail(String to, String firstName, String lastName, String emailVerificationUrl) throws MessagingException {
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper messageHelper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
        messageHelper.setFrom(senderEmail);

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
            messageHelper.setText(htmlContent, true);

            messageHelper.setTo(to);
            sender.send(message);
            log.info("Welcome email sent email={}", MaskingUtil.maskEmail(to));
        } catch (Exception e) {
            log.error("Failed to send welcome email to email={}", MaskingUtil.maskEmail(to), e);
            throw e;
        }
    }
}
