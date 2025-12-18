package com.baskaaleksander.nuvine.infrastructure.messaging.in;

import com.baskaaleksander.nuvine.domain.service.EmbeddingService;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.VectorProcessingRequestDlqMessage;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.VectorProcessingRequestDlqProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class VectorProcessingRequestDlqWorker {

    private final EmbeddingService embeddingService;
    private final VectorProcessingRequestDlqProducer dlqProducer;

    @Value("${dlq.vector-processing-request.max-retry-attempts:10}")
    private int maxRetryAttempts;

    @KafkaListener(
            topics = "${topics.vector-processing-request-dlq-topic}",
            containerFactory = "vectorProcessingRequestDlqKafkaListenerContainerFactory"
    )
    public void processDlqBatch(List<VectorProcessingRequestDlqMessage> messages) {
        log.info("VECTOR_PROCESSING_REQUEST_DLQ_WORKER BATCH RECEIVED size={}", messages.size());

        for (VectorProcessingRequestDlqMessage dlqMessage : messages) {
            processMessage(dlqMessage);
        }

        log.info("VECTOR_PROCESSING_REQUEST_DLQ_WORKER BATCH PROCESSED size={}", messages.size());
    }

    private void processMessage(VectorProcessingRequestDlqMessage dlqMessage) {
        String ingestionJobId = dlqMessage.originalEvent().ingestionJobId();
        int attemptCount = dlqMessage.attemptCount();

        log.info("VECTOR_PROCESSING_REQUEST_DLQ_WORKER PROCESSING ingestionJobId={} attemptCount={}", ingestionJobId, attemptCount);

        try {
            embeddingService.process(dlqMessage.originalEvent());
            log.info("VECTOR_PROCESSING_REQUEST_DLQ_WORKER SUCCESS ingestionJobId={} attemptCount={}", ingestionJobId, attemptCount);
        } catch (Exception e) {
            log.error("VECTOR_PROCESSING_REQUEST_DLQ_WORKER FAILED ingestionJobId={} attemptCount={} error={}",
                    ingestionJobId, attemptCount, e.getMessage(), e);

            handleRetryOrDeadLetter(dlqMessage, e);
        }
    }

    private void handleRetryOrDeadLetter(VectorProcessingRequestDlqMessage dlqMessage, Exception e) {
        String ingestionJobId = dlqMessage.originalEvent().ingestionJobId();
        VectorProcessingRequestDlqMessage updatedMessage = dlqMessage.incrementAttempt(e);

        if (isPermanentFailure(e)) {
            log.error("VECTOR_PROCESSING_REQUEST_DLQ_WORKER PERMANENT_FAILURE ingestionJobId={} error={} - moving to dead letter",
                    ingestionJobId, e.getMessage());
            dlqProducer.sendToDeadLetter(updatedMessage);
            return;
        }

        if (updatedMessage.attemptCount() >= maxRetryAttempts) {
            log.error("VECTOR_PROCESSING_REQUEST_DLQ_WORKER MAX_RETRIES_EXCEEDED ingestionJobId={} attemptCount={} firstFailedAt={} - moving to dead letter",
                    ingestionJobId, updatedMessage.attemptCount(), dlqMessage.firstFailedAt());
            dlqProducer.sendToDeadLetter(updatedMessage);
        } else {
            log.warn("VECTOR_PROCESSING_REQUEST_DLQ_WORKER RETRY_SCHEDULED ingestionJobId={} attemptCount={} nextAttempt={}",
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
