package com.baskaaleksander.nuvine.infrastructure.messaging.out;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DocumentIngestionDlqMessage;
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
public class DocumentIngestionDlqProducer {

    @Value("${topics.document-ingestion-dlq-topic}")
    private String dlqTopic;

    @Value("${topics.document-ingestion-dead-letter-topic}")
    private String deadLetterTopic;

    private final KafkaTemplate<String, DocumentIngestionDlqMessage> kafkaTemplate;

    public void sendToDlq(DocumentIngestionDlqMessage dlqMessage) {
        log.warn("Sending event to DLQ topic={} documentId={} attemptCount={}",
                dlqTopic,
                dlqMessage.originalEvent().documentId(),
                dlqMessage.attemptCount());

        Message<DocumentIngestionDlqMessage> message = MessageBuilder
                .withPayload(dlqMessage)
                .setHeader(KafkaHeaders.TOPIC, dlqTopic)
                .setHeader(KafkaHeaders.KEY, dlqMessage.originalEvent().documentId())
                .build();

        kafkaTemplate.send(message);
    }

    public void sendToDeadLetter(DocumentIngestionDlqMessage dlqMessage) {
        log.error("Sending event to DEAD LETTER topic={} documentId={} attemptCount={} firstFailedAt={} lastFailedAt={}",
                deadLetterTopic,
                dlqMessage.originalEvent().documentId(),
                dlqMessage.attemptCount(),
                dlqMessage.firstFailedAt(),
                dlqMessage.lastFailedAt());

        Message<DocumentIngestionDlqMessage> message = MessageBuilder
                .withPayload(dlqMessage)
                .setHeader(KafkaHeaders.TOPIC, deadLetterTopic)
                .setHeader(KafkaHeaders.KEY, dlqMessage.originalEvent().documentId())
                .build();

        kafkaTemplate.send(message);
    }
}
