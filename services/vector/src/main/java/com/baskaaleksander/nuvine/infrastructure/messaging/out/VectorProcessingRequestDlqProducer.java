package com.baskaaleksander.nuvine.infrastructure.messaging.out;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.VectorProcessingRequestDlqMessage;
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
public class VectorProcessingRequestDlqProducer {

    @Value("${topics.vector-processing-request-dlq-topic}")
    private String dlqTopic;

    @Value("${topics.vector-processing-request-dead-letter-topic}")
    private String deadLetterTopic;

    private final KafkaTemplate<String, VectorProcessingRequestDlqMessage> kafkaTemplate;

    public void sendToDlq(VectorProcessingRequestDlqMessage dlqMessage) {
        log.warn("Sending event to DLQ topic={} ingestionJobId={} attemptCount={}",
                dlqTopic,
                dlqMessage.originalEvent().ingestionJobId(),
                dlqMessage.attemptCount());

        Message<VectorProcessingRequestDlqMessage> message = MessageBuilder
                .withPayload(dlqMessage)
                .setHeader(KafkaHeaders.TOPIC, dlqTopic)
                .setHeader(KafkaHeaders.KEY, dlqMessage.originalEvent().ingestionJobId())
                .build();

        kafkaTemplate.send(message);
    }

    public void sendToDeadLetter(VectorProcessingRequestDlqMessage dlqMessage) {
        log.error("Sending event to DEAD LETTER topic={} ingestionJobId={} attemptCount={} firstFailedAt={} lastFailedAt={}",
                deadLetterTopic,
                dlqMessage.originalEvent().ingestionJobId(),
                dlqMessage.attemptCount(),
                dlqMessage.firstFailedAt(),
                dlqMessage.lastFailedAt());

        Message<VectorProcessingRequestDlqMessage> message = MessageBuilder
                .withPayload(dlqMessage)
                .setHeader(KafkaHeaders.TOPIC, deadLetterTopic)
                .setHeader(KafkaHeaders.KEY, dlqMessage.originalEvent().ingestionJobId())
                .build();

        kafkaTemplate.send(message);
    }
}
