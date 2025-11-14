package com.baskaaleksander.nuvine.infrastructure.messaging.out;

import com.baskaaleksander.nuvine.domain.service.NotificationEntityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationDlqConsumer {

    private final NotificationEntityService service;

    @KafkaListener(
            topics = {
                    "${topics.email-verification-topic}.DLT",
                    "${topics.password-reset-topic}.DLT",
                    "${topics.user-registered-topic}.DLT"
            },
            groupId = "${spring.kafka.consumer.group-id:notification-service}-dlq"
    )
    public void handleDlqMessage() {

    }
}
