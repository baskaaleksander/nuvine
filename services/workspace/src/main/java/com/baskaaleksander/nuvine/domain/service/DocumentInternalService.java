package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.DocumentInternalResponse;
import com.baskaaleksander.nuvine.application.mapper.DocumentMapper;
import com.baskaaleksander.nuvine.domain.exception.DocumentNotFoundException;
import com.baskaaleksander.nuvine.domain.model.Document;
import com.baskaaleksander.nuvine.domain.model.DocumentStatus;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DocumentUploadedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.DocumentUploadedEventProducer;
import com.baskaaleksander.nuvine.infrastructure.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentInternalService {

    private final DocumentRepository documentRepository;
    private final DocumentMapper documentMapper;
    private final DocumentUploadedEventProducer eventProducer;
    private final EntityCacheEvictionService entityCacheEvictionService;

    @Cacheable(value = "entity-document-internal", key = "#id.toString()")
    public DocumentInternalResponse getDocumentById(UUID id) {
        Document document = documentRepository.findById(id).orElseThrow(() -> new DocumentNotFoundException("Document not found"));
        if (document.isDeleted()) {
            throw new DocumentNotFoundException("Document not found");
        }
        return documentMapper.toInternalResponse(document);
    }

    public DocumentInternalResponse uploadCompleted(
            UUID documentId,
            String storageKey,
            String mimeType,
            Long sizeBytes
    ) {
        var document = documentRepository.findById(documentId).orElseThrow(() -> new DocumentNotFoundException("Document not found"));
        if (document.isDeleted()) {
            throw new DocumentNotFoundException("Document not found");
        }
        document.setStorageKey(storageKey);
        document.setMimeType(mimeType);
        document.setSizeBytes(sizeBytes);
        document.setStatus(DocumentStatus.UPLOADED);
        documentRepository.save(document);
        entityCacheEvictionService.evictDocument(documentId);

        eventProducer.sendDocumentUploadedEvent(
                new DocumentUploadedEvent(
                        documentId.toString(),
                        document.getWorkspaceId().toString(),
                        document.getProjectId().toString(),
                        storageKey,
                        mimeType,
                        sizeBytes
                )
        );

        return documentMapper.toInternalResponse(document);
    }

    public DocumentInternalResponse updateStatus(
            UUID documentId,
            DocumentStatus status
    ) {
        var document = documentRepository.findById(documentId).orElseThrow(() -> new DocumentNotFoundException("Document not found"));

        if (document.isDeleted()) {
            throw new DocumentNotFoundException("Document not found");
        }

        document.setStatus(status);
        documentRepository.save(document);
        entityCacheEvictionService.evictDocument(documentId);
        return documentMapper.toInternalResponse(document);
    }
}
