package com.baskaaleksander.nuvine.infrastructure.messaging.out;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.WorkspaceMemberDataUpdateDlqMessage;
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
public class WorkspaceMemberDataUpdateDlqProducer {

    @Value("${topics.workspace-member-data-update-dlq-topic}")
    private String dlqTopic;

    @Value("${topics.workspace-member-data-update-dead-letter-topic}")
    private String deadLetterTopic;

    private final KafkaTemplate<String, WorkspaceMemberDataUpdateDlqMessage> kafkaTemplate;

    public void sendToDlq(WorkspaceMemberDataUpdateDlqMessage dlqMessage) {
        log.warn("Sending event to DLQ topic={} userId={} attemptCount={}",
                dlqTopic,
                dlqMessage.originalEvent().userId(),
                dlqMessage.attemptCount());

        Message<WorkspaceMemberDataUpdateDlqMessage> message = MessageBuilder
                .withPayload(dlqMessage)
                .setHeader(KafkaHeaders.TOPIC, dlqTopic)
                .setHeader(KafkaHeaders.KEY, dlqMessage.originalEvent().userId())
                .build();

        kafkaTemplate.send(message);
    }

    public void sendToDeadLetter(WorkspaceMemberDataUpdateDlqMessage dlqMessage) {
        log.error("Sending event to DEAD LETTER topic={} userId={} attemptCount={} firstFailedAt={} lastFailedAt={}",
                deadLetterTopic,
                dlqMessage.originalEvent().userId(),
                dlqMessage.attemptCount(),
                dlqMessage.firstFailedAt(),
                dlqMessage.lastFailedAt());

        Message<WorkspaceMemberDataUpdateDlqMessage> message = MessageBuilder
                .withPayload(dlqMessage)
                .setHeader(KafkaHeaders.TOPIC, deadLetterTopic)
                .setHeader(KafkaHeaders.KEY, dlqMessage.originalEvent().userId())
                .build();

        kafkaTemplate.send(message);
    }
}
