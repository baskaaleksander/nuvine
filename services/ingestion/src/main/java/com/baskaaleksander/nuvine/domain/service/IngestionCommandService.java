package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.DocumentInternalResponse;
import com.baskaaleksander.nuvine.domain.exception.DocumentNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.IngestionJobConflictException;
import com.baskaaleksander.nuvine.domain.exception.UnauthorizedDocumentAccessException;
import com.baskaaleksander.nuvine.domain.model.IngestionJob;
import com.baskaaleksander.nuvine.domain.model.IngestionStage;
import com.baskaaleksander.nuvine.domain.model.IngestionStatus;
import com.baskaaleksander.nuvine.infrastructure.client.WorkspaceServiceClient;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DocumentUploadedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.DocumentUploadedEventProducer;
import com.baskaaleksander.nuvine.infrastructure.repository.IngestionJobRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class IngestionCommandService {

    private final WorkspaceServiceClient workspaceServiceClient;
    private final IngestionService ingestionService;
    private final DocumentUploadedEventProducer producer;
    private final IngestionJobRepository ingestionJobRepository;

    public void startIngestionJob(String documentId) {
        DocumentInternalResponse document = fetchDocumentOrThrow(documentId);

        DocumentUploadedEvent event = new DocumentUploadedEvent(
                document.id().toString(),
                document.workspaceId().toString(),
                document.projectId().toString(),
                document.storageKey(),
                document.mimeType(),
                document.sizeBytes()
        );

        producer.sendDocumentUploadedEvent(event);
    }

    public void retryIngestionJob(String documentId) {
        IngestionJob job = ingestionJobRepository.findByDocumentId(UUID.fromString(documentId))
                .orElseThrow(() -> new DocumentNotFoundException("Document not found"));

        if (job.getStatus() != IngestionStatus.FAILED) {
            log.info("INGESTION_PROCESS END reason=job_not_failed documentId={}", documentId);
            throw new IngestionJobConflictException("Job is not failed");
        }

        job.setRetryCount(job.getRetryCount() + 1);
        job.setStatus(IngestionStatus.QUEUED);
        job.setStage(IngestionStage.QUEUED);
        job.setLastError(null);

        ingestionJobRepository.save(job);

        startIngestionJob(documentId);
    }

    private DocumentInternalResponse fetchDocumentOrThrow(String documentId) {
        try {
            return workspaceServiceClient.getInternalDocument(documentId);
        } catch (FeignException ex) {
            int status = ex.status();
            String message = ex.getMessage();
            log.info(message);
            if (status == 404) {
                throw new DocumentNotFoundException("Document not found");
            } else if (status == 401 || status == 403) {
                throw new UnauthorizedDocumentAccessException("Unauthorized document access");
            } else {
                throw new RuntimeException("Failed to get document", ex);
            }
        }
    }
}
