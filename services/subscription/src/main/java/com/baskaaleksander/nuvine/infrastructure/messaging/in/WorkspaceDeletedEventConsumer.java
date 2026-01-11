package com.baskaaleksander.nuvine.infrastructure.messaging.in;

import com.baskaaleksander.nuvine.domain.service.SubscriptionService;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.WorkspaceDeletedDlqMessage;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.WorkspaceDeletedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.WorkspaceDeletedDlqProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class WorkspaceDeletedEventConsumer {

    private final SubscriptionService subscriptionService;
    private final WorkspaceDeletedDlqProducer dlqProducer;

    @Value("${topics.workspace-deleted-topic}")
    private String workspaceDeletedTopic;

    @KafkaListener(topics = "${topics.workspace-deleted-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(WorkspaceDeletedEvent event) {
        log.info("Received WorkspaceDeletedEvent for workspaceId: {}", event.workspaceId());
        try {
            subscriptionService.cancelSubscription(event.stripeSubscriptionId());
            log.info("Successfully processed WorkspaceDeletedEvent for workspaceId: {}", event.workspaceId());
        } catch (Exception e) {
            log.error("Error processing WorkspaceDeletedEvent for workspaceId: {}. Sending to DLQ.", event.workspaceId(), e);
            WorkspaceDeletedDlqMessage dlqMessage = WorkspaceDeletedDlqMessage.createInitial(event, e, workspaceDeletedTopic);
            dlqProducer.sendToDlq(dlqMessage);
        }
    }
}
