package com.baskaaleksander.nuvine.infrastructure.messaging;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DocumentUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentUploadedEventConsumer {
f
    @KafkaListener(topics = "${topics.document-uploaded-topic}", groupId = "${spring.kafka.consumer.group-id:ingestion-service}")
    public void consumeDocumentUploadedEvent(DocumentUploadedEvent event) {
        log.info("Consumed document uploaded event: {}", event);
    }

}
