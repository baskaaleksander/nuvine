package com.baskaaleksander.nuvine.infrastructure.messaging.in;

import com.baskaaleksander.nuvine.domain.service.DocumentStatusOrchestrator;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DocumentIngestionCompletedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DocumentIngestionDlqMessage;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.DocumentIngestionDlqProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentIngestionCompletedEventConsumer {

    private final DocumentStatusOrchestrator documentStatusOrchestrator;
    private final DocumentIngestionDlqProducer documentIngestionDlqProducer;

    @Value("${topics.document-ingestion-completed-topic}")
    private String documentIngestionCompletedTopic;

    @KafkaListener(topics = "${topics.document-ingestion-completed-topic}")
    public void consumeDocumentIngestionCompletedEvent(DocumentIngestionCompletedEvent event) {
        log.info("DOCUMENT_INGESTION_COMPLETED_EVENT received documentId={} projectId={} workspaceId={}", event.documentId(), event.projectId(), event.workspaceId());

        try {
            documentStatusOrchestrator.handleDocumentIngestionCompleted(event.documentId());
            log.info("DOCUMENT_INGESTION_COMPLETED_EVENT processed documentId={} projectId={} workspaceId={}", event.documentId(), event.projectId(), event.workspaceId());
        } catch (Exception e) {
            log.error("DOCUMENT_INGESTION_COMPLETED_EVENT failed documentId={} error={}",
                    event.documentId(), e.getMessage(), e);

            DocumentIngestionDlqMessage dlqMessage = DocumentIngestionDlqMessage.createInitial(event, e, documentIngestionCompletedTopic);
            documentIngestionDlqProducer.sendToDlq(dlqMessage);

            log.info("DOCUMENT_INGESTION_COMPLETED_EVENT sent to DLQ documentId={}", event.documentId());
        }
    }
}
