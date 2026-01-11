package com.baskaaleksander.nuvine.infrastructure.messaging.in;

import com.baskaaleksander.nuvine.domain.service.SubscriptionService;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.WorkspaceDeletedDlqMessage;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.WorkspaceDeletedDlqProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class WorkspaceDeletedDlqWorker {

    private final SubscriptionService subscriptionService;
    private final WorkspaceDeletedDlqProducer dlqProducer;

    @Value("${dlq.workspace-deleted.max-retry-attempts:10}")
    private int maxRetryAttempts;

    @KafkaListener(
            topics = "${topics.workspace-deleted-dlq-topic}",
            groupId = "subscription-workspace-deleted-dlq-worker",
            containerFactory = "workspaceDeletedDlqKafkaListenerContainerFactory"
    )
    public void processDlqBatch(List<WorkspaceDeletedDlqMessage> messages) {
        log.info("Processing {} workspace-deleted DLQ messages", messages.size());

        for (WorkspaceDeletedDlqMessage dlqMessage : messages) {
            try {
                processMessage(dlqMessage);
            } catch (Exception e) {
                log.error("Failed to process DLQ message: {}", dlqMessage, e);
                // Individual message failure handling logic if needed, 
                // but typically we try to process and if it fails again, handleRetryOrDeadLetter is called inside processMessage.
                // Wait, processMessage catches its own exceptions? No, let's make sure we handle it.
            }
        }
    }

    private void processMessage(WorkspaceDeletedDlqMessage dlqMessage) {
        try {
            log.info("Retrying processing for workspaceId: {}, attempt: {}", 
                    dlqMessage.originalEvent().workspaceId(), dlqMessage.attemptCount());
            
            subscriptionService.cancelSubscription(dlqMessage.originalEvent().stripeSubscriptionId());
            
            log.info("Successfully processed DLQ message for workspaceId: {}", dlqMessage.originalEvent().workspaceId());
        } catch (Exception e) {
            handleRetryOrDeadLetter(dlqMessage, e);
        }
    }

    private void handleRetryOrDeadLetter(WorkspaceDeletedDlqMessage dlqMessage, Exception e) {
        if (dlqMessage.attemptCount() >= maxRetryAttempts) {
            log.error("Max retry attempts reached for workspaceId: {}. Sending to Dead Letter Topic.", 
                    dlqMessage.originalEvent().workspaceId(), e);
            dlqProducer.sendToDeadLetter(dlqMessage);
        } else {
            log.warn("Retry failed for workspaceId: {}. Incrementing attempt count and re-queueing.", 
                    dlqMessage.originalEvent().workspaceId(), e);
            WorkspaceDeletedDlqMessage updatedMessage = dlqMessage.incrementAttempt(e);
            dlqProducer.sendToDlq(updatedMessage);
        }
    }
}
