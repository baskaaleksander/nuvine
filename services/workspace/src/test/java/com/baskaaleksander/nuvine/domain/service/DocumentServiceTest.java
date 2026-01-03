package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.DocumentFilterRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private DocumentMapper documentMapper;
    @Mock
    private EntityCacheEvictionService entityCacheEvictionService;

    @InjectMocks
    private DocumentService documentService;

    private UUID projectId;
    private UUID workspaceId;
    private UUID documentId;
    private UUID userId;
    private Project project;
    private Document savedDocument;
    private Document deletedDocument;
    private DocumentPublicResponse documentResponse;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        workspaceId = UUID.randomUUID();
        documentId = UUID.randomUUID();
        userId = UUID.randomUUID();

        project = Project.builder()
                .id(projectId)
                .workspaceId(workspaceId)
                .name("Project")
                .description("Desc")
                .deleted(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        savedDocument = Document.builder()
                .id(documentId)
                .projectId(projectId)
                .workspaceId(workspaceId)
                .name("Doc")
                .status(DocumentStatus.UPLOADING)
                .createdBy(userId)
                .deleted(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        documentResponse = new DocumentPublicResponse(
                savedDocument.getId(),
                savedDocument.getProjectId(),
                savedDocument.getWorkspaceId(),
                savedDocument.getName(),
                savedDocument.getStatus(),
                savedDocument.getCreatedBy(),
                savedDocument.getCreatedAt()
        );

        deletedDocument = Document.builder()
                .id(documentId)
                .projectId(projectId)
                .workspaceId(workspaceId)
                .name("Doc")
                .status(DocumentStatus.UPLOADING)
                .createdBy(userId)
                .deleted(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void createDocument_whenProjectNotFound_throwsProjectNotFoundException() {
        when(projectRepository.findById(projectId)).thenReturn(java.util.Optional.empty());

        assertThrows(ProjectNotFoundException.class,
                () -> documentService.createDocument("Doc", userId, projectId));

        verify(projectRepository).findById(projectId);
        verify(documentRepository, never()).save(any());
        verifyNoInteractions(documentMapper);
    }

    @Test
    void createDocument_savesWithProjectWorkspaceAndMaps() {
        when(projectRepository.findById(projectId)).thenReturn(java.util.Optional.of(project));
        when(documentRepository.save(any(Document.class))).thenReturn(savedDocument);
        when(documentMapper.toDocumentResponse(savedDocument)).thenReturn(documentResponse);

        DocumentPublicResponse response = documentService.createDocument("Doc", userId, projectId);

        ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(docCaptor.capture());
        Document persisted = docCaptor.getValue();

        assertEquals(projectId, persisted.getProjectId());
        assertEquals(workspaceId, persisted.getWorkspaceId());
        assertEquals("Doc", persisted.getName());
        assertEquals(DocumentStatus.UPLOADING, persisted.getStatus());
        assertEquals(userId, persisted.getCreatedBy());
        assertEquals(documentResponse, response);
    }

    @Test
    void getDocuments_returnsPagedResponseWithMappedDocuments() {
        PaginationRequest request = new PaginationRequest(0, 5, "name", Sort.Direction.ASC);
        DocumentFilterRequest filter = new DocumentFilterRequest(null, null, null);
        Pageable pageable = PaginationUtil.getPageable(request);

        Page<Document> page = new PageImpl<>(List.of(savedDocument), pageable, 1);

        when(documentRepository.findAllByProjectIdWithFilters(projectId, null, null, null, pageable)).thenReturn(page);
        when(documentMapper.toDocumentResponse(savedDocument)).thenReturn(documentResponse);

        PagedResponse<DocumentPublicResponse> response = documentService.getDocuments(projectId, request, filter);

        verify(documentRepository).findAllByProjectIdWithFilters(projectId, null, null, null, pageable);

        assertEquals(1, response.content().size());
        assertEquals(documentResponse, response.content().iterator().next());
        assertEquals(page.getTotalPages(), response.totalPages());
        assertEquals(page.getTotalElements(), response.totalElements());
        assertEquals(page.getSize(), response.size());
        assertEquals(page.getNumber(), response.page());
        assertEquals(page.isLast(), response.last());
        assertEquals(page.hasNext(), response.next());
        verify(documentMapper).toDocumentResponse(savedDocument);
        verifyNoMoreInteractions(documentMapper);
    }

    @Test
    void getDocuments_whenEmpty_returnsEmptyContent() {
        PaginationRequest request = new PaginationRequest(0, 5, "name", Sort.Direction.ASC);
        DocumentFilterRequest filter = new DocumentFilterRequest(null, null, null);
        Pageable pageable = PaginationUtil.getPageable(request);
        Page<Document> page = new PageImpl<>(List.of(), pageable, 0);

        when(documentRepository.findAllByProjectIdWithFilters(projectId, null, null, null, pageable)).thenReturn(page);

        PagedResponse<DocumentPublicResponse> response = documentService.getDocuments(projectId, request, filter);

        assertEquals(0, response.content().size());
        assertEquals(0, response.totalElements());
        assertEquals(0, response.totalPages());
        verifyNoInteractions(documentMapper);
    }

    @Test
    void getDocument_whenNotFound_throwsDocumentNotFoundException() {
        when(documentRepository.findById(documentId)).thenReturn(java.util.Optional.empty());

        assertThrows(DocumentNotFoundException.class, () -> documentService.getDocument(documentId));

        verify(documentRepository).findById(documentId);
        verifyNoInteractions(documentMapper);
    }

    @Test
    void getDocument_whenDeleted_throwsDocumentNotFoundException() {
        when(documentRepository.findById(documentId)).thenReturn(java.util.Optional.of(deletedDocument));

        assertThrows(DocumentNotFoundException.class, () -> documentService.getDocument(documentId));

        verify(documentRepository).findById(documentId);
        verifyNoInteractions(documentMapper);
    }

    @Test
    void getDocument_returnsMappedResponse() {
        when(documentRepository.findById(documentId)).thenReturn(java.util.Optional.of(savedDocument));
        when(documentMapper.toDocumentResponse(savedDocument)).thenReturn(documentResponse);

        DocumentPublicResponse response = documentService.getDocument(documentId);

        assertEquals(documentResponse, response);
        verify(documentRepository).findById(documentId);
        verify(documentMapper).toDocumentResponse(savedDocument);
    }

    @Test
    void updateDocument_whenNotFound_throwsDocumentNotFoundException() {
        when(documentRepository.findById(documentId)).thenReturn(java.util.Optional.empty());

        assertThrows(DocumentNotFoundException.class, () -> documentService.updateDocument(documentId, "New Name"));

        verify(documentRepository).findById(documentId);
        verify(documentRepository, never()).save(any());
        verifyNoInteractions(documentMapper);
    }

    @Test
    void updateDocument_whenDeleted_throwsDocumentNotFoundException() {
        when(documentRepository.findById(documentId)).thenReturn(java.util.Optional.of(deletedDocument));

        assertThrows(DocumentNotFoundException.class, () -> documentService.updateDocument(documentId, "New Name"));

        verify(documentRepository).findById(documentId);
        verify(documentRepository, never()).save(any());
        verifyNoInteractions(documentMapper);
    }

    @Test
    void updateDocument_whenNameUnchanged_throwsDocumentConflictException() {
        when(documentRepository.findById(documentId)).thenReturn(java.util.Optional.of(savedDocument));

        assertThrows(DocumentConflictException.class, () -> documentService.updateDocument(documentId, savedDocument.getName()));

        verify(documentRepository).findById(documentId);
        verify(documentRepository, never()).save(any());
        verifyNoInteractions(documentMapper);
    }

    @Test
    void updateDocument_updatesNameAndMapsResponse() {
        when(documentRepository.findById(documentId)).thenReturn(java.util.Optional.of(savedDocument));
        when(documentRepository.save(any(Document.class))).thenReturn(savedDocument);
        when(documentMapper.toDocumentResponse(savedDocument)).thenReturn(documentResponse);

        DocumentPublicResponse response = documentService.updateDocument(documentId, "New Name");

        ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(docCaptor.capture());
        Document updated = docCaptor.getValue();

        assertEquals("New Name", updated.getName());
        assertEquals(documentResponse, response);
    }

    @Test
    void deleteDocument_whenNotFound_throwsDocumentNotFoundException() {
        when(documentRepository.findById(documentId)).thenReturn(java.util.Optional.empty());

        assertThrows(DocumentNotFoundException.class, () -> documentService.deleteDocument(documentId));

        verify(documentRepository).findById(documentId);
        verify(documentRepository, never()).save(any());
    }

    @Test
    void deleteDocument_whenDeleted_throwsDocumentNotFoundException() {
        when(documentRepository.findById(documentId)).thenReturn(java.util.Optional.of(deletedDocument));

        assertThrows(DocumentNotFoundException.class, () -> documentService.deleteDocument(documentId));

        verify(documentRepository).findById(documentId);
        verify(documentRepository, never()).save(any());
    }

    @Test
    void deleteDocument_setsDeletedTrueAndSaves() {
        when(documentRepository.findById(documentId)).thenReturn(java.util.Optional.of(savedDocument));

        documentService.deleteDocument(documentId);

        ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(docCaptor.capture());
        assertTrue(docCaptor.getValue().isDeleted());
    }
}
