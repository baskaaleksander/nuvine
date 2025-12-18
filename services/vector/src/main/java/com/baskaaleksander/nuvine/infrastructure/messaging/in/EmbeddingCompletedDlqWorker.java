package com.baskaaleksander.nuvine.infrastructure.messaging.in;

import com.baskaaleksander.nuvine.domain.service.EmbeddingService;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmbeddingCompletedDlqMessage;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.EmbeddingCompletedDlqProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmbeddingCompletedDlqWorker {

    private final EmbeddingService embeddingService;
    private final EmbeddingCompletedDlqProducer dlqProducer;

    @Value("${dlq.embedding-completed.max-retry-attempts:10}")
    private int maxRetryAttempts;

    @KafkaListener(
            topics = "${topics.embedding-completed-dlq-topic}",
            containerFactory = "dlqKafkaListenerContainerFactory"
    )
    public void processDlqBatch(List<EmbeddingCompletedDlqMessage> messages) {
        log.info("EMBEDDING_DLQ_WORKER BATCH RECEIVED size={}", messages.size());

        for (EmbeddingCompletedDlqMessage dlqMessage : messages) {
            processMessage(dlqMessage);
        }

        log.info("EMBEDDING_DLQ_WORKER BATCH PROCESSED size={}", messages.size());
    }

    private void processMessage(EmbeddingCompletedDlqMessage dlqMessage) {
        String jobId = dlqMessage.originalEvent().ingestionJobId();
        int attemptCount = dlqMessage.attemptCount();

        log.info("EMBEDDING_DLQ_WORKER PROCESSING jobId={} attemptCount={}", jobId, attemptCount);

        try {
            embeddingService.processEmbeddingCompletedEvent(dlqMessage.originalEvent());
            log.info("EMBEDDING_DLQ_WORKER SUCCESS jobId={} attemptCount={}", jobId, attemptCount);
        } catch (Exception e) {
            log.error("EMBEDDING_DLQ_WORKER FAILED jobId={} attemptCount={} error={}",
                    jobId, attemptCount, e.getMessage(), e);

            handleRetryOrDeadLetter(dlqMessage, e);
        }
    }

    private void handleRetryOrDeadLetter(EmbeddingCompletedDlqMessage dlqMessage, Exception e) {
        String jobId = dlqMessage.originalEvent().ingestionJobId();
        EmbeddingCompletedDlqMessage updatedMessage = dlqMessage.incrementAttempt(e);

        if (isPermanentFailure(e)) {
            log.error("EMBEDDING_DLQ_WORKER PERMANENT_FAILURE jobId={} error={} - moving to dead letter",
                    jobId, e.getMessage());
            dlqProducer.sendToDeadLetter(updatedMessage);
            return;
        }

        if (updatedMessage.attemptCount() >= maxRetryAttempts) {
            log.error("EMBEDDING_DLQ_WORKER MAX_RETRIES_EXCEEDED jobId={} attemptCount={} firstFailedAt={} - moving to dead letter",
                    jobId, updatedMessage.attemptCount(), dlqMessage.firstFailedAt());
            dlqProducer.sendToDeadLetter(updatedMessage);
        } else {
            log.warn("EMBEDDING_DLQ_WORKER RETRY_SCHEDULED jobId={} attemptCount={} nextAttempt={}",
                    jobId, updatedMessage.attemptCount(), updatedMessage.attemptCount() + 1);
            dlqProducer.sendToDlq(updatedMessage);
        }
    }

    private boolean isPermanentFailure(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }

        return message.contains("Job not found") ||
               message.contains("not found") ||
               e instanceof IllegalArgumentException;
    }
}
