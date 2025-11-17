package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.DocumentInternalResponse;
import com.baskaaleksander.nuvine.application.mapper.DocumentMapper;
import com.baskaaleksander.nuvine.domain.model.DocumentStatus;
import com.baskaaleksander.nuvine.infrastructure.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentInternalService {

    private final DocumentRepository documentRepository;
    private final DocumentMapper documentMapper;

    public DocumentInternalResponse getDocumentById(UUID id) {
        return documentMapper.toInternalResponse(documentRepository.findById(id).orElseThrow());
    }

    public DocumentInternalResponse uploadCompleted(
            UUID documentId,
            String storageKey,
            String mimeType,
            Long sizeBytes
    ) {
        var document = documentRepository.findById(documentId).orElseThrow();
        document.setStorageKey(storageKey);
        document.setMimeType(mimeType);
        document.setSizeBytes(sizeBytes);
        document.setStatus(DocumentStatus.UPLOADED);
        documentRepository.save(document);
        return documentMapper.toInternalResponse(document);
    }

    public DocumentInternalResponse updateStatus(
            UUID documentId,
            DocumentStatus status
    ) {
        var document = documentRepository.findById(documentId).orElseThrow();
        document.setStatus(status);
        documentRepository.save(document);
        return documentMapper.toInternalResponse(document);
    }
}
