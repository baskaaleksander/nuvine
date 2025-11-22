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

        log.info("INGESTION_PROCESS START documentId={}", event.documentId());

        job = updateIngestionJobStatus(job, IngestionStatus.PROCESSING);
        job = updateIngestionJobStage(job, IngestionStage.FETCH);

        byte[] document;

        try {
            document = documentFetcher.fetch(event.storageKey());
        } catch (Exception e) {
            log.error("INGESTION_PROCESS FETCHING_FAILED documentId={}", event.documentId(), e);
            updateIngestionJobStatus(job, IngestionStatus.FAILED);
            throw e;
        }

        job = updateIngestionJobStage(job, IngestionStage.PARSE);
        log.info("INGESTION_PROCESS DOCUMENT_FETCHED size={}", document.length);

        ExtractedDocument extractedDocument;
        try {
            log.info("INGESTION_PROCESS EXTRACTION_START documentId={}", event.documentId());
            extractedDocument = extractionService.extract(document, event.mimeType());
        } catch (Exception e) {
            log.error("INGESTION_PROCESS EXTRACTION_FAILED documentId={}", event.documentId(), e);
            updateIngestionJobStatus(job, IngestionStatus.FAILED);
            return;
        }

        job = updateIngestionJobStage(job, IngestionStage.CHUNK);
        log.info("INGESTION_PROCESS EXTRACTION_SUCCESS documentId={} text-length={}", event.documentId(), extractedDocument.text().length());

        List<Chunk> chunks;

        try {
            log.info("INGESTION_PROCESS CHUNKING_START documentId={}", event.documentId());
            chunks = chunkerService.chunkDocument(extractedDocument, UUID.fromString(event.documentId()));
        } catch (Exception e) {
            log.error("INGESTION_PROCESS CHUNKING_FAILED documentId={}", event.documentId(), e);
            updateIngestionJobStatus(job, IngestionStatus.FAILED);
            return;
        }

        log.info("INGESTION_PROCESS CHUNKING_SUCCESS documentId={} chunk-count={}", event.documentId(), chunks.size());


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
