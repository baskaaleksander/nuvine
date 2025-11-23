package com.baskaaleksander.nuvine.infrastructure.messaging.in;

import com.baskaaleksander.nuvine.domain.exception.IngestionJobNotFoundException;
import com.baskaaleksander.nuvine.domain.model.IngestionJob;
import com.baskaaleksander.nuvine.domain.model.IngestionStatus;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DocumentUploadedEvent;
import com.baskaaleksander.nuvine.infrastructure.repository.IngestionJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class DocumentUploadDlqConsumer {

    private final IngestionJobRepository jobRepository;

    @KafkaListener(
            topics = "${topics.document-uploaded-topic}.DLT"
    )
    public void consumeDocumentUploadDlq(
            DocumentUploadedEvent event,
            @Header(KafkaHeaders.DLT_ORIGINAL_TOPIC) String originalTopic,
            @Header(KafkaHeaders.DLT_ORIGINAL_PARTITION) int originalPartition,
            @Header(KafkaHeaders.DLT_ORIGINAL_OFFSET) long originalOffset,
            @Header(KafkaHeaders.DELIVERY_ATTEMPT) Integer deliveryAttempt
    ) {
        UUID documentId = UUID.fromString(event.documentId());

        IngestionJob job = jobRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new IngestionJobNotFoundException("Document not found"));

        job.setStatus(IngestionStatus.FAILED);
        job.setLastError("FAILED_AFTER_MAX_RETRIES (attempts=" + deliveryAttempt + ")");

        jobRepository.save(job);

        log.error(
                "INGESTION_PROCESS PERMANENT_FAILURE documentId={} originalTopic={} partition={} offset={} attempts={}",
                documentId, originalTopic, originalPartition, originalOffset, deliveryAttempt
        );
    }
}
