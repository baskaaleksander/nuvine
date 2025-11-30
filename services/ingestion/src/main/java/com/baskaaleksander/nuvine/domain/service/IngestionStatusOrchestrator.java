package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.domain.model.IngestionJob;
import com.baskaaleksander.nuvine.domain.model.IngestionStatus;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DocumentIngestionCompletedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.DocumentIngestionCompletedEventProducer;
import com.baskaaleksander.nuvine.infrastructure.repository.IngestionJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class IngestionStatusOrchestrator {

    private final IngestionJobRepository ingestionJobRepository;
    private final DocumentIngestionCompletedEventProducer documentIngestionCompletedEventProducer;

    public void handleVectorProcessingCompleted(String ingestionJobId) {
        log.info("VECTOR_PROCESSING_COMPLETED_EVENT received ingestionJobId={}", ingestionJobId);
        IngestionJob job = ingestionJobRepository.findById(UUID.fromString(ingestionJobId)).orElseGet(() -> null);

        if (job == null) {
            log.warn("INGESTION_JOB_NOT_FOUND ingestionJobId={}", ingestionJobId);
            return;
        }

        job.setStatus(IngestionStatus.COMPLETED);

        documentIngestionCompletedEventProducer.sendDocumentIngestionCompletedEvent(
                new DocumentIngestionCompletedEvent(
                        job.getDocumentId().toString(),
                        job.getWorkspaceId().toString(),
                        job.getProjectId().toString()
                )
        );

        ingestionJobRepository.save(job);
    }
}
