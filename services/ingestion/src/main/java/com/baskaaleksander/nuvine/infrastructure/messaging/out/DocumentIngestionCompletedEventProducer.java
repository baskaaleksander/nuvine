package com.baskaaleksander.nuvine.infrastructure.messaging.out;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DocumentIngestionCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentIngestionCompletedEventProducer {

    private final KafkaTemplate<String, DocumentIngestionCompletedEvent> kafkaTemplate;

    @Value("${topics.document-ingestion-completed-topic}")
    private String topic;

    public void sendDocumentIngestionCompletedEvent(DocumentIngestionCompletedEvent event) {
        log.info("DOCUMENT_INGESTION_COMPLETED_EVENT START documentId={} projectId={} workspaceId={}", event.documentId(), event.projectId(), event.workspaceId());
        Message<DocumentIngestionCompletedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .build();
        kafkaTemplate.send(message);
        log.info("DOCUMENT_INGESTION_COMPLETED_EVENT END documentId={} projectId={} workspaceId={}", event.documentId(), event.projectId(), event.workspaceId());
    }
}
