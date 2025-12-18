package com.baskaaleksander.nuvine.infrastructure.messaging.in;

import com.baskaaleksander.nuvine.domain.service.UsageService;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DlqMessage;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.UsageDlqProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UsageDlqWorker {

    private final UsageService usageService;
    private final UsageDlqProducer usageDlqProducer;

    @Value("${dlq.usage-logs.max-retry-attempts:10}")
    private int maxRetryAttempts;

    @KafkaListener(
            topics = "${topics.usage-logs-dlq-topic}",
            containerFactory = "dlqKafkaListenerContainerFactory"
    )
    public void processDlqBatch(List<DlqMessage> messages) {
        log.info("DLQ_WORKER BATCH RECEIVED size={}", messages.size());

        for (DlqMessage dlqMessage : messages) {
            processMessage(dlqMessage);
        }

        log.info("DLQ_WORKER BATCH PROCESSED size={}", messages.size());
    }

    private void processMessage(DlqMessage dlqMessage) {
        String workspaceId = dlqMessage.originalEvent().workspaceId();
        int attemptCount = dlqMessage.attemptCount();

        log.info("DLQ_WORKER PROCESSING workspaceId={} attemptCount={}", workspaceId, attemptCount);

        try {
            usageService.logTokenUsage(dlqMessage.originalEvent());
            log.info("DLQ_WORKER SUCCESS workspaceId={} attemptCount={}", workspaceId, attemptCount);
        } catch (Exception e) {
            log.error("DLQ_WORKER FAILED workspaceId={} attemptCount={} error={}",
                    workspaceId, attemptCount, e.getMessage(), e);

            handleRetryOrDeadLetter(dlqMessage, e);
        }
    }

    private void handleRetryOrDeadLetter(DlqMessage dlqMessage, Exception e) {
        String workspaceId = dlqMessage.originalEvent().workspaceId();
        DlqMessage updatedMessage = dlqMessage.incrementAttempt(e);

        if (updatedMessage.attemptCount() >= maxRetryAttempts) {
            log.error("DLQ_WORKER MAX_RETRIES_EXCEEDED workspaceId={} attemptCount={} firstFailedAt={} - moving to dead letter",
                    workspaceId, updatedMessage.attemptCount(), dlqMessage.firstFailedAt());

            usageDlqProducer.sendToDeadLetter(updatedMessage);
        } else {
            log.warn("DLQ_WORKER RETRY_SCHEDULED workspaceId={} attemptCount={} nextAttempt={}",
                    workspaceId, updatedMessage.attemptCount(), updatedMessage.attemptCount() + 1);

            usageDlqProducer.sendToDlq(updatedMessage);
        }
    }
}
