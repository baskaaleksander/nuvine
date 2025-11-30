package com.baskaaleksander.nuvine.infrastructure.messaging.in;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DocumentIngestionCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentIngestionCompletedEventConsumer {

    @KafkaListener(topics = "${topics.document-ingestion-completed-topic}")
    public void consumeDocumentIngestionCompletedEvent(DocumentIngestionCompletedEvent event) {
        log.info("DOCUMENT_INGESTION_COMPLETED_EVENT received documentId={} projectId={} workspaceId={}", event.documentId(), event.projectId(), event.workspaceId());
    }
}
