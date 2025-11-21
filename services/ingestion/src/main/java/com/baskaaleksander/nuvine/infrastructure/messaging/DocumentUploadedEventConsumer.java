package com.baskaaleksander.nuvine.infrastructure.messaging;

import com.baskaaleksander.nuvine.domain.service.IngestionService;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DocumentUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentUploadedEventConsumer {

    private final IngestionService ingestionService;

    @KafkaListener(topics = "${topics.document-uploaded-topic}", groupId = "${spring.kafka.consumer.group-id:ingestion-service}")
    public void consumeDocumentUploadedEvent(DocumentUploadedEvent event) {
        log.info("DOCUMENT_UPLOADED_EVENT RECEIVED documentId={}", event.documentId());
        ingestionService.process(event);
        log.info("DOCUMENT_UPLOAD_EVENT HANDLED documentId={}", event.documentId());
    }

}
