package com.baskaaleksander.nuvine.infrastructure.messaging.out;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DlqMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UsageDlqProducer {

    private final KafkaTemplate<String, DlqMessage> kafkaTemplate;

    @Value("${topics.usage-logs-dlq-topic}")
    private String dlqTopic;

    @Value("${topics.usage-logs-dead-letter-topic}")
    private String deadLetterTopic;

    public void sendToDlq(DlqMessage dlqMessage) {
        log.warn("Sending event to DLQ topic={} workspaceId={} attemptCount={}",
                dlqTopic,
                dlqMessage.originalEvent().workspaceId(),
                dlqMessage.attemptCount());

        Message<DlqMessage> message = MessageBuilder
                .withPayload(dlqMessage)
                .setHeader(KafkaHeaders.TOPIC, dlqTopic)
                .setHeader(KafkaHeaders.KEY, dlqMessage.originalEvent().workspaceId())
                .build();

        kafkaTemplate.send(message);
    }

    public void sendToDeadLetter(DlqMessage dlqMessage) {
        log.error("Sending event to DEAD LETTER topic={} workspaceId={} attemptCount={} firstFailedAt={} lastFailedAt={}",
                deadLetterTopic,
                dlqMessage.originalEvent().workspaceId(),
                dlqMessage.attemptCount(),
                dlqMessage.firstFailedAt(),
                dlqMessage.lastFailedAt());

        Message<DlqMessage> message = MessageBuilder
                .withPayload(dlqMessage)
                .setHeader(KafkaHeaders.TOPIC, deadLetterTopic)
                .setHeader(KafkaHeaders.KEY, dlqMessage.originalEvent().workspaceId())
                .build();

        kafkaTemplate.send(message);
    }
}
