package com.baskaaleksander.nuvine.infrastructure.messaging.in;

import com.baskaaleksander.nuvine.domain.service.IngestionStatusOrchestrator;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.VectorProcessingCompletedDlqMessage;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.VectorProcessingCompletedDlqProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class VectorProcessingCompletedDlqWorker {

    private final IngestionStatusOrchestrator ingestionStatusOrchestrator;
    private final VectorProcessingCompletedDlqProducer dlqProducer;

    @Value("${dlq.vector-processing-completed.max-retry-attempts:10}")
    private int maxRetryAttempts;

    @KafkaListener(
            topics = "${topics.vector-processing-completed-dlq-topic}",
            containerFactory = "vectorProcessingCompletedDlqKafkaListenerContainerFactory"
    )
    public void processDlqBatch(List<VectorProcessingCompletedDlqMessage> messages) {
        log.info("VECTOR_PROCESSING_COMPLETED_DLQ_WORKER BATCH RECEIVED size={}", messages.size());

        for (VectorProcessingCompletedDlqMessage dlqMessage : messages) {
            processMessage(dlqMessage);
        }

        log.info("VECTOR_PROCESSING_COMPLETED_DLQ_WORKER BATCH PROCESSED size={}", messages.size());
    }

    private void processMessage(VectorProcessingCompletedDlqMessage dlqMessage) {
        String ingestionJobId = dlqMessage.originalEvent().ingestionJobId();
        int attemptCount = dlqMessage.attemptCount();

        log.info("VECTOR_PROCESSING_COMPLETED_DLQ_WORKER PROCESSING ingestionJobId={} attemptCount={}", ingestionJobId, attemptCount);

        try {
            ingestionStatusOrchestrator.handleVectorProcessingCompleted(ingestionJobId);
            log.info("VECTOR_PROCESSING_COMPLETED_DLQ_WORKER SUCCESS ingestionJobId={} attemptCount={}", ingestionJobId, attemptCount);
        } catch (Exception e) {
            log.error("VECTOR_PROCESSING_COMPLETED_DLQ_WORKER FAILED ingestionJobId={} attemptCount={} error={}",
                    ingestionJobId, attemptCount, e.getMessage(), e);

            handleRetryOrDeadLetter(dlqMessage, e);
        }
    }

    private void handleRetryOrDeadLetter(VectorProcessingCompletedDlqMessage dlqMessage, Exception e) {
        String ingestionJobId = dlqMessage.originalEvent().ingestionJobId();
        VectorProcessingCompletedDlqMessage updatedMessage = dlqMessage.incrementAttempt(e);

        if (isPermanentFailure(e)) {
            log.error("VECTOR_PROCESSING_COMPLETED_DLQ_WORKER PERMANENT_FAILURE ingestionJobId={} error={} - moving to dead letter",
                    ingestionJobId, e.getMessage());
            dlqProducer.sendToDeadLetter(updatedMessage);
            return;
        }

        if (updatedMessage.attemptCount() >= maxRetryAttempts) {
            log.error("VECTOR_PROCESSING_COMPLETED_DLQ_WORKER MAX_RETRIES_EXCEEDED ingestionJobId={} attemptCount={} firstFailedAt={} - moving to dead letter",
                    ingestionJobId, updatedMessage.attemptCount(), dlqMessage.firstFailedAt());
            dlqProducer.sendToDeadLetter(updatedMessage);
        } else {
            log.warn("VECTOR_PROCESSING_COMPLETED_DLQ_WORKER RETRY_SCHEDULED ingestionJobId={} attemptCount={} nextAttempt={}",
                    ingestionJobId, updatedMessage.attemptCount(), updatedMessage.attemptCount() + 1);
            dlqProducer.sendToDlq(updatedMessage);
        }
    }

    private boolean isPermanentFailure(Exception e) {
        // UUID parsing failures
        if (e instanceof IllegalArgumentException) {
            return true;
        }

        // Missing required fields
        if (e instanceof NullPointerException) {
            return true;
        }

        String message = e.getMessage();
        if (message == null) {
            return false;
        }

        // Invalid input data
        return message.contains("Invalid UUID") ||
               message.contains("cannot be null") ||
               message.contains("must not be null");
    }
}
