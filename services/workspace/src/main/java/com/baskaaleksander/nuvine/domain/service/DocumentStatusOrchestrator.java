package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.domain.model.Document;
import com.baskaaleksander.nuvine.domain.model.DocumentStatus;
import com.baskaaleksander.nuvine.infrastructure.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentStatusOrchestrator {

    private final DocumentRepository documentRepository;
    private final EntityCacheEvictionService entityCacheEvictionService;

    public void handleDocumentIngestionCompleted(String documentId) {
        log.info("DOCUMENT_INGESTION_COMPLETED_EVENT received documentId={}", documentId);
        Document document = documentRepository.findById(UUID.fromString(documentId)).orElseGet(() -> null);

        if (document == null) {
            log.warn("DOCUMENT_NOT_FOUND documentId={}", documentId);
            return;
        }

        document.setStatus(DocumentStatus.PROCESSED);

        documentRepository.save(document);
        entityCacheEvictionService.evictDocument(UUID.fromString(documentId));
    }
}
