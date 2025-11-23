package com.baskaaleksander.nuvine.infrastructure.messaging.out;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DocumentUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DocumentUploadedEventProducer {

    @Value("${topics.document-uploaded-topic}")
    private String topic;

    private final KafkaTemplate<String, DocumentUploadedEvent> kafkaTemplate;

    public void sendDocumentUploadedEvent(DocumentUploadedEvent event) {
        log.info("DOCUMENT_UPLOADED EVENT START documentId={}", event.documentId());

        Message<DocumentUploadedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .build();

        kafkaTemplate.send(message);

        log.info("DOCUMENT_UPLOADED EVENT END documentId={}", event.documentId());
    }
}
