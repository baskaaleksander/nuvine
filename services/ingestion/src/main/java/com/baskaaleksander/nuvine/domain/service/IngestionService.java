package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.domain.model.ExtractedDocument;
import com.baskaaleksander.nuvine.domain.model.IngestionJob;
import com.baskaaleksander.nuvine.domain.model.IngestionStage;
import com.baskaaleksander.nuvine.domain.model.IngestionStatus;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DocumentUploadedEvent;
import com.baskaaleksander.nuvine.infrastructure.repository.IngestionJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static java.util.UUID.fromString;

@Service
@RequiredArgsConstructor
@Slf4j
public class IngestionService {

    private final IngestionJobRepository ingestionJobRepository;
    private final DocumentFetcher documentFetcher;
    private final ExtractionService extractionService;

    public void process(DocumentUploadedEvent event) {
        IngestionJob job = createIngestionJob(event);

        log.info("INGESTION_PROCESS START documentId={}", event.documentId());

        job = updateIngestionJobStatus(job, IngestionStatus.PROCESSING);
        job = updateIngestionJobStage(job, IngestionStage.FETCH);

        byte[] document = documentFetcher.fetch(event.storageKey());

        job = updateIngestionJobStage(job, IngestionStage.PARSE);
        log.info("INGESTION_PROCESS DOCUMENT_FETCHED size={}", document.length);

        ExtractedDocument extractedDocument;
        try {
            extractedDocument = extractionService.extract(document, event.mimeType());
        } catch (Exception e) {
            log.error("INGESTION_PROCESS EXTRACTION_FAILED documentId={}", event.documentId(), e);
            updateIngestionJobStatus(job, IngestionStatus.FAILED);
            throw e;
        }

        log.info("INGESTION_PROCESS EXTRACTION_SUCCESS text={}", extractedDocument.text());


        log.info("INGESTION_PROCESS END documentId={}", event.documentId());

    }

    private IngestionJob createIngestionJob(DocumentUploadedEvent event) {

        IngestionJob job = IngestionJob.builder()
                .documentId(fromString(event.documentId()))
                .workspaceId(fromString(event.workspaceId()))
                .projectId(fromString(event.projectId()))
                .storageKey(event.storageKey())
                .mimeType(event.mimeType())
                .status(IngestionStatus.QUEUED)
                .stage(IngestionStage.QUEUED)
                .build();

        return ingestionJobRepository.save(job);
    }

    private IngestionJob updateIngestionJobStatus(IngestionJob job, IngestionStatus status) {
        job.setStatus(status);
        return ingestionJobRepository.save(job);
    }

    private IngestionJob updateIngestionJobStage(IngestionJob job, IngestionStage stage) {
        job.setStage(stage);
        return ingestionJobRepository.save(job);
    }
}
