package com.baskaaleksander.nuvine.domain.service;

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

    public void process(DocumentUploadedEvent event) {
        log.info("INGESTION_PROCESS START documentId={}", event.documentId());

        byte[] document = documentFetcher.fetch(event.storageKey());

        log.info("INGESTION_PROCESS DOCUMENT_FETCHED size={}", document.length);

        log.info("INGESTION_PROCESS END documentId={}", event.documentId());

        IngestionJob job = IngestionJob.builder()
                .documentId(fromString(event.documentId()))
                .workspaceId(fromString(event.workspaceId()))
                .projectId(fromString(event.projectId()))
                .storageKey(event.storageKey())
                .mimeType(event.mimeType())
                .status(IngestionStatus.COMPLETED)
                .stage(IngestionStage.QUEUED)
                .build();

        ingestionJobRepository.save(job);
    }
}
