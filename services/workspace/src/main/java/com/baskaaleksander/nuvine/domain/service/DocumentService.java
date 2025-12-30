package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.DocumentPublicResponse;
import com.baskaaleksander.nuvine.application.dto.PagedResponse;
import com.baskaaleksander.nuvine.application.dto.PaginationRequest;
import com.baskaaleksander.nuvine.application.mapper.DocumentMapper;
import com.baskaaleksander.nuvine.application.pagination.PaginationUtil;
import com.baskaaleksander.nuvine.domain.exception.DocumentConflictException;
import com.baskaaleksander.nuvine.domain.exception.DocumentNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.ProjectNotFoundException;
import com.baskaaleksander.nuvine.domain.model.Document;
import com.baskaaleksander.nuvine.domain.model.DocumentStatus;
import com.baskaaleksander.nuvine.domain.model.Project;
import com.baskaaleksander.nuvine.infrastructure.repository.DocumentRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ProjectRepository projectRepository;
    private final DocumentMapper documentMapper;
    private final EntityCacheEvictionService entityCacheEvictionService;

    public DocumentPublicResponse createDocument(String name, UUID userId, UUID projectId) {
        log.info("CREATE_DOCUMENT START projectId={}", projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> {
                    log.info("CREATE_DOCUMENT FAILED reason=project_not_found projectId={}", projectId);
                    return new ProjectNotFoundException("Project not found");
                });

        Document document = Document.builder()
                .projectId(projectId)
                .workspaceId(project.getWorkspaceId())
                .name(name)
                .status(DocumentStatus.UPLOADING)
                .createdBy(userId)
                .projectId(projectId)
                .build();

        Document documentSaved = documentRepository.save(document);
        log.info("CREATE_DOCUMENT END documentId={} projectId={}", documentSaved.getId(), projectId);

        return documentMapper.toDocumentResponse(documentSaved);
    }

    public PagedResponse<DocumentPublicResponse> getDocuments(UUID projectId, PaginationRequest request) {
        log.info("GET_DOCUMENTS START projectId={}", projectId);
        Pageable pageable = PaginationUtil.getPageable(request);
        Page<Document> page = documentRepository.findAllByProjectId(projectId, pageable);

        List<DocumentPublicResponse> content = page.getContent().stream()
                .map(documentMapper::toDocumentResponse)
                .toList();

        log.info("GET_DOCUMENTS END projectId={}", projectId);
        return new PagedResponse<>(
                content,
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize(),
                page.getNumber(),
                page.isLast(),
                page.hasNext()
        );
    }

    @Cacheable(value = "entity-document", key = "#documentId.toString()")
    public DocumentPublicResponse getDocument(UUID documentId) {
        log.info("GET_DOCUMENT START documentId={}", documentId);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> {
                    log.info("GET_DOCUMENT FAILED reason=document_not_found documentId={}", documentId);
                    return new DocumentNotFoundException("Document not found");
                });

        if (document.isDeleted()) {
            log.info("GET_DOCUMENT FAILED reason=document_deleted documentId={}", documentId);
            throw new DocumentNotFoundException("Document not found");
        }

        log.info("GET_DOCUMENT END documentId={}", documentId);

        return documentMapper.toDocumentResponse(document);
    }

    public DocumentPublicResponse updateDocument(UUID documentId, String name) {
        log.info("UPDATE_DOCUMENT START documentId={}", documentId);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> {
                    log.info("UPDATE_DOCUMENT FAILED reason=document_not_found documentId={}", documentId);
                    return new DocumentNotFoundException("Document not found");
                });

        if (document.isDeleted()) {
            log.info("UPDATE_DOCUMENT FAILED reason=document_deleted documentId={}", documentId);
            throw new DocumentNotFoundException("Document not found");
        }

        if (document.getName().equals(name)) {
            log.info("UPDATE_DOCUMENT FAILED reason=document_name_not_changed documentId={}", documentId);
            throw new DocumentConflictException("Document is already named like that");
        }

        document.setName(name);
        Document documentSaved = documentRepository.save(document);
        entityCacheEvictionService.evictDocument(documentId);
        log.info("UPDATE_DOCUMENT END documentId={}", documentId);

        return documentMapper.toDocumentResponse(documentSaved);
    }

    public void deleteDocument(UUID documentId) {
        log.info("DELETE_DOCUMENT START documentId={}", documentId);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> {
                    log.info("DELETE_DOCUMENT FAILED reason=document_not_found documentId={}", documentId);
                    return new DocumentNotFoundException("Document not found");
                });

        if (document.isDeleted()) {
            log.info("DELETE_DOCUMENT FAILED reason=document_deleted documentId={}", documentId);
            throw new DocumentNotFoundException("Document not found");
        }

        document.setDeleted(true);
        documentRepository.save(document);
        entityCacheEvictionService.evictDocument(documentId);

        log.info("DELETE_DOCUMENT END documentId={}", documentId);
    }
}
