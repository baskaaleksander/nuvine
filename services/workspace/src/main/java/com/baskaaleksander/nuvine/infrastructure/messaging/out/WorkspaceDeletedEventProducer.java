package com.baskaaleksander.nuvine.infrastructure.messaging.out;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.WorkspaceDeletedEvent;
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
public class WorkspaceDeletedEventProducer {

    @Value("${topics.workspace-deleted-topic}")
    private String topic;

    private final KafkaTemplate<String, WorkspaceDeletedEvent> kafkaTemplate;

    public void send(WorkspaceDeletedEvent event) {
        log.info("Sending WorkspaceDeletedEvent for workspaceId: {}", event.workspaceId());
        
        Message<WorkspaceDeletedEvent> message = MessageBuilder.withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.KEY, event.workspaceId().toString())
                .build();
                
        kafkaTemplate.send(message);
        log.info("WorkspaceDeletedEvent sent successfully for workspaceId: {}", event.workspaceId());
    }
}
