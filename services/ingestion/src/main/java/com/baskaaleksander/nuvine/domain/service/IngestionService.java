package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.domain.model.*;
import com.baskaaleksander.nuvine.domain.service.chunker.ChunkerService;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DocumentUploadedEvent;
import com.baskaaleksander.nuvine.infrastructure.repository.IngestionJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static java.util.UUID.fromString;

@Service
@RequiredArgsConstructor
@Slf4j
public class IngestionService {

    private final IngestionJobRepository ingestionJobRepository;
    private final DocumentFetcher documentFetcher;
    private final ExtractionService extractionService;
    private final ChunkerService chunkerService;

    public void process(DocumentUploadedEvent event) {
        IngestionJob job = createIngestionJob(event);

        if (job.getStatus() == IngestionStatus.COMPLETED) {
            log.info("INGESTION_PROCESS END reason=job_completed documentId={}", event.documentId());
            return;
        }

        log.info("INGESTION_PROCESS START documentId={}", event.documentId());

        job = updateIngestionJobStatus(job, IngestionStatus.PROCESSING);

        byte[] document = executeStage(
                job,
                IngestionStage.FETCH,
                "FETCHING",
                true,
                () -> documentFetcher.fetch(event.storageKey())
        );

        ExtractedDocument extractedDocument = executeStage(
                job,
                IngestionStage.PARSE,
                "EXTRACTION",
                false,
                () -> extractionService.extract(document, event.mimeType())
        );

        if (extractedDocument == null) {
            log.info("INGESTION_PROCESS FAILED reason=extracted_document_null documentId={}", event.documentId());
            return;
        }

        List<Chunk> chunks = executeStage(
                job,
                IngestionStage.CHUNK,
                "CHUNKING",
                false,
                () -> chunkerService.chunkDocument(extractedDocument, UUID.fromString(event.documentId()))
        );

        if (chunks == null) {
            log.error("INGESTION_PROCESS CHUNKING_SKIPPED chunks_null documentId={}", event.documentId());
            return;
        }

        log.info("INGESTION_PROCESS CHUNKING_RESULT size={} documentId={}", chunks.size(), event.documentId());

        job = updateIngestionJobStatus(job, IngestionStatus.COMPLETED);

        log.info("INGESTION_PROCESS END documentId={}", event.documentId());
    }

    private IngestionJob createIngestionJob(DocumentUploadedEvent event) {

        return ingestionJobRepository.findByDocumentId(UUID.fromString(event.documentId()))
                .orElseGet(() -> {
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
                );
    }

    private IngestionJob updateIngestionJobStatus(IngestionJob job, IngestionStatus status) {
        IngestionJob currentJob = ingestionJobRepository.findById(job.getId())
                .orElseThrow(() -> new IllegalStateException("Job not found: " + job.getId()));

        currentJob.setStatus(status);
        return ingestionJobRepository.save(currentJob);
    }

    private IngestionJob updateIngestionJobStage(IngestionJob job, IngestionStage stage) {
        IngestionJob currentJob = ingestionJobRepository.findById(job.getId())
                .orElseThrow(() -> new IllegalStateException("Job not found: " + job.getId()));

        currentJob.setStage(stage);
        return ingestionJobRepository.save(currentJob);
    }

    private void saveError(IngestionJob job, String message, boolean incrementRetryCount) {
        job.setLastError(message);

        if (incrementRetryCount) {
            job.setRetryCount(job.getRetryCount() + 1);
        } else {
            job.setStatus(IngestionStatus.FAILED);
        }

        ingestionJobRepository.save(job);
    }

    private <T> T executeStage(
            IngestionJob job,
            IngestionStage stage,
            String actionName,
            boolean retryable,
            Supplier<T> supplier
    ) {
        try {
            job = updateIngestionJobStage(job, stage);
            log.info("INGESTION_PROCESS {}_START documentId={}", actionName, job.getDocumentId());

            T result = supplier.get();
            log.info("INGESTION_PROCESS {}_SUCCESS documentId={}", actionName, job.getDocumentId());
            return result;
        } catch (Exception e) {
            log.error("INGESTION_PROCESS {}_FAILED stage={} documentId={}",
                    actionName, stage, job != null ? job.getDocumentId() : null, e);

            if (retryable) {
                saveError(job, e.getMessage(), true);
                throw e;
            } else {
                updateIngestionJobStatus(job, IngestionStatus.FAILED);
                saveError(job, e.getMessage(), false);
                return null;
            }
        }
    }
}
