package com.baskaaleksander.nuvine.infrastructure.messaging.in;

import com.baskaaleksander.nuvine.domain.service.DocumentStatusOrchestrator;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DocumentIngestionDlqMessage;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.DocumentIngestionDlqProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentIngestionDlqWorker {

    private final DocumentStatusOrchestrator documentStatusOrchestrator;
    private final DocumentIngestionDlqProducer documentIngestionDlqProducer;

    @Value("${dlq.document-ingestion.max-retry-attempts:10}")
    private int maxRetryAttempts;

    @KafkaListener(
            topics = "${topics.document-ingestion-dlq-topic}",
            containerFactory = "dlqKafkaListenerContainerFactory"
    )
    public void processDlqBatch(List<DocumentIngestionDlqMessage> messages) {
        log.info("DLQ_WORKER BATCH RECEIVED size={}", messages.size());

        for (DocumentIngestionDlqMessage dlqMessage : messages) {
            processMessage(dlqMessage);
        }

        log.info("DLQ_WORKER BATCH PROCESSED size={}", messages.size());
    }

    private void processMessage(DocumentIngestionDlqMessage dlqMessage) {
        String documentId = dlqMessage.originalEvent().documentId();
        int attemptCount = dlqMessage.attemptCount();

        log.info("DLQ_WORKER PROCESSING documentId={} attemptCount={}", documentId, attemptCount);

        try {
            documentStatusOrchestrator.handleDocumentIngestionCompleted(documentId);
            log.info("DLQ_WORKER SUCCESS documentId={} attemptCount={}", documentId, attemptCount);
        } catch (Exception e) {
            log.error("DLQ_WORKER FAILED documentId={} attemptCount={} error={}",
                    documentId, attemptCount, e.getMessage(), e);

            handleRetryOrDeadLetter(dlqMessage, e);
        }
    }

    private void handleRetryOrDeadLetter(DocumentIngestionDlqMessage dlqMessage, Exception e) {
        String documentId = dlqMessage.originalEvent().documentId();
        DocumentIngestionDlqMessage updatedMessage = dlqMessage.incrementAttempt(e);

        if (updatedMessage.attemptCount() >= maxRetryAttempts) {
            log.error("DLQ_WORKER MAX_RETRIES_EXCEEDED documentId={} attemptCount={} firstFailedAt={} - moving to dead letter",
                    documentId, updatedMessage.attemptCount(), dlqMessage.firstFailedAt());

            documentIngestionDlqProducer.sendToDeadLetter(updatedMessage);
        } else {
            log.warn("DLQ_WORKER RETRY_SCHEDULED documentId={} attemptCount={} nextAttempt={}",
                    documentId, updatedMessage.attemptCount(), updatedMessage.attemptCount() + 1);

            documentIngestionDlqProducer.sendToDlq(updatedMessage);
        }
    }
}
