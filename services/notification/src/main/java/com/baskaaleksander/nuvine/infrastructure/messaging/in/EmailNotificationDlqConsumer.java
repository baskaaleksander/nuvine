package com.baskaaleksander.nuvine.infrastructure.messaging.in;

import com.baskaaleksander.nuvine.application.dto.CreateFailedNotificationRequest;
import com.baskaaleksander.nuvine.domain.model.NotificationType;
import com.baskaaleksander.nuvine.domain.service.NotificationEntityService;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmailVerificationEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.PasswordResetEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.UserRegisteredEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationDlqConsumer {

    private final NotificationEntityService service;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = {
                    "${topics.email-verification-topic}.DLT",
                    "${topics.password-reset-topic}.DLT",
                    "${topics.user-registered-topic}.DLT"
            },
            groupId = "${spring.kafka.consumer.group-id:notification-service}-dlq"
    )
    public void handleDlq(
            ConsumerRecord<String, Object> record,
            @Header(KafkaHeaders.DLT_ORIGINAL_TOPIC) String originalTopic,
            @Header(KafkaHeaders.DLT_EXCEPTION_MESSAGE) String exceptionMessage,
            @Header(KafkaHeaders.DLT_EXCEPTION_FQCN) String exceptionClass
    ) {
        Object value = record.value();
        String topic = record.topic();

        log.error("DLQ event from topic={} partition={} offset={} payload={}",
                originalTopic,
                record.partition(),
                record.offset(),
                value
        );

        try {
            CreateFailedNotificationRequest request = mapToFailedNotificationRequest(
                    value,
                    originalTopic,
                    String.valueOf(record.partition()),
                    record.offset(),
                    exceptionMessage,
                    exceptionClass
            );

            service.saveFailedFromDlq(request);

        } catch (Exception e) {
            log.error("Failed to map DLQ event payload={} topic={}", value, topic, e);
        }
    }

    private CreateFailedNotificationRequest mapToFailedNotificationRequest(
            Object payload,
            String originalTopic,
            String originalPartition,
            Long originalOffset,
            String exceptionMessage,
            String exceptionClass
    ) throws Exception {

        String payloadJson = payload != null
                ? objectMapper.writeValueAsString(payload)
                : null;

        if (payload instanceof EmailVerificationEvent event) {
            return new CreateFailedNotificationRequest(
                    event.userId(),
                    NotificationType.EMAIL_VERIFICATION,
                    payloadJson,
                    originalTopic,
                    originalPartition,
                    originalOffset,
                    exceptionMessage,
                    exceptionClass
            );
        }

        if (payload instanceof PasswordResetEvent event) {
            return new CreateFailedNotificationRequest(
                    event.userId(),
                    NotificationType.PASSWORD_RESET,
                    payloadJson,
                    originalTopic,
                    originalPartition,
                    originalOffset,
                    exceptionMessage,
                    exceptionClass
            );
        }

        if (payload instanceof UserRegisteredEvent event) {
            return new CreateFailedNotificationRequest(
                    event.userId(),
                    NotificationType.USER_REGISTERED,
                    payloadJson,
                    originalTopic,
                    originalPartition,
                    originalOffset,
                    exceptionMessage,
                    exceptionClass
            );
        }

        return new CreateFailedNotificationRequest(
                null,
                null,
                payloadJson,
                originalTopic,
                originalPartition,
                originalOffset,
                exceptionMessage,
                exceptionClass
        );
    }
}
