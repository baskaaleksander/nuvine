package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.DocumentInternalResponse;
import com.baskaaleksander.nuvine.domain.exception.DocumentNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.UnauthorizedDocumentAccessException;
import com.baskaaleksander.nuvine.infrastructure.client.WorkspaceServiceClient;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DocumentUploadedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.DocumentUploadedEventProducer;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class IngestionCommandService {

    private final WorkspaceServiceClient workspaceServiceClient;
    private final IngestionService ingestionService;
    private final DocumentUploadedEventProducer producer;

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
