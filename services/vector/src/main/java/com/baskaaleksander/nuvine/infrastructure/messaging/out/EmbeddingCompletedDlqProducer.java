package com.baskaaleksander.nuvine.infrastructure.messaging.out;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmbeddingCompletedDlqMessage;
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
public class EmbeddingCompletedDlqProducer {

    @Value("${topics.embedding-completed-dlq-topic}")
    private String dlqTopic;

    @Value("${topics.embedding-completed-dead-letter-topic}")
    private String deadLetterTopic;

    private final KafkaTemplate<String, EmbeddingCompletedDlqMessage> kafkaTemplate;

    public void sendToDlq(EmbeddingCompletedDlqMessage dlqMessage) {
        log.warn("Sending event to DLQ topic={} jobId={} attemptCount={}",
                dlqTopic,
                dlqMessage.originalEvent().ingestionJobId(),
                dlqMessage.attemptCount());

        Message<EmbeddingCompletedDlqMessage> message = MessageBuilder
                .withPayload(dlqMessage)
                .setHeader(KafkaHeaders.TOPIC, dlqTopic)
                .setHeader(KafkaHeaders.KEY, dlqMessage.originalEvent().ingestionJobId())
                .build();

        kafkaTemplate.send(message);
    }

    public void sendToDeadLetter(EmbeddingCompletedDlqMessage dlqMessage) {
        log.error("Sending event to DEAD LETTER topic={} jobId={} attemptCount={} firstFailedAt={} lastFailedAt={}",
                deadLetterTopic,
                dlqMessage.originalEvent().ingestionJobId(),
                dlqMessage.attemptCount(),
                dlqMessage.firstFailedAt(),
                dlqMessage.lastFailedAt());

        Message<EmbeddingCompletedDlqMessage> message = MessageBuilder
                .withPayload(dlqMessage)
                .setHeader(KafkaHeaders.TOPIC, deadLetterTopic)
                .setHeader(KafkaHeaders.KEY, dlqMessage.originalEvent().ingestionJobId())
                .build();

        kafkaTemplate.send(message);
    }
}
