package com.baskaaleksander.nuvine.infrastructure.messaging.out;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.WorkspaceDeletedDlqMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class WorkspaceDeletedDlqProducer {

    @Value("${topics.workspace-deleted-dlq-topic}")
    private String dlqTopic;

    @Value("${topics.workspace-deleted-dead-letter-topic}")
    private String deadLetterTopic;

    private final KafkaTemplate<String, WorkspaceDeletedDlqMessage> kafkaTemplate;

    public void sendToDlq(WorkspaceDeletedDlqMessage dlqMessage) {
        log.info("Sending message to DLQ: {}", dlqMessage);

        Message<WorkspaceDeletedDlqMessage> message = MessageBuilder
                .withPayload(dlqMessage)
                .setHeader(KafkaHeaders.TOPIC, dlqTopic)
                .setHeader(KafkaHeaders.KEY, dlqMessage.originalEvent().workspaceId().toString())
                .build();

        kafkaTemplate.send(message);
    }

    public void sendToDeadLetter(WorkspaceDeletedDlqMessage dlqMessage) {
        log.error("Sending message to Dead Letter Queue: {}", dlqMessage);

        Message<WorkspaceDeletedDlqMessage> message = MessageBuilder
                .withPayload(dlqMessage)
                .setHeader(KafkaHeaders.TOPIC, deadLetterTopic)
                .setHeader(KafkaHeaders.KEY, dlqMessage.originalEvent().workspaceId().toString())
                .build();

        kafkaTemplate.send(message);
    }
}
