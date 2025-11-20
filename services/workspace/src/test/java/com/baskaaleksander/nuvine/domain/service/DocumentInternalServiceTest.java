package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.DocumentInternalResponse;
import com.baskaaleksander.nuvine.application.mapper.DocumentMapper;
import com.baskaaleksander.nuvine.domain.exception.DocumentNotFoundException;
import com.baskaaleksander.nuvine.domain.model.Document;
import com.baskaaleksander.nuvine.domain.model.DocumentStatus;
import com.baskaaleksander.nuvine.infrastructure.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentInternalServiceTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentMapper documentMapper;

    @InjectMocks
    private DocumentInternalService documentInternalService;

    private UUID documentId;
    private UUID projectId;
    private UUID workspaceId;
    private UUID userId;
    private Document activeDocument;
    private Document deletedDocument;
    private DocumentInternalResponse internalResponse;

    @BeforeEach
    void setUp() {
        documentId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        workspaceId = UUID.randomUUID();
        userId = UUID.randomUUID();

        activeDocument = Document.builder()
                .id(documentId)
                .projectId(projectId)
                .workspaceId(workspaceId)
                .name("Doc")
                .status(DocumentStatus.UPLOADING)
                .storageKey("key")
                .mimeType("text/plain")
                .sizeBytes(123L)
                .createdBy(userId)
                .deleted(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

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

        internalResponse = new DocumentInternalResponse(
                activeDocument.getId(),
                activeDocument.getProjectId(),
                activeDocument.getWorkspaceId(),
                activeDocument.getName(),
                activeDocument.getStatus(),
                activeDocument.getStorageKey(),
                activeDocument.getMimeType(),
                activeDocument.getSizeBytes(),
                activeDocument.getCreatedBy(),
                activeDocument.getCreatedAt()
        );
    }

    @Test
    void getDocumentById_whenNotFound_throwsDocumentNotFoundException() {
        when(documentRepository.findById(documentId)).thenReturn(java.util.Optional.empty());

        assertThrows(DocumentNotFoundException.class, () -> documentInternalService.getDocumentById(documentId));

        verify(documentRepository).findById(documentId);
        verifyNoInteractions(documentMapper);
    }

    @Test
    void getDocumentById_whenDeleted_throwsDocumentNotFoundException() {
        when(documentRepository.findById(documentId)).thenReturn(java.util.Optional.of(deletedDocument));

        assertThrows(DocumentNotFoundException.class, () -> documentInternalService.getDocumentById(documentId));

        verify(documentRepository).findById(documentId);
        verifyNoInteractions(documentMapper);
    }

    @Test
    void getDocumentById_returnsInternalResponse() {
        when(documentRepository.findById(documentId)).thenReturn(java.util.Optional.of(activeDocument));
        when(documentMapper.toInternalResponse(activeDocument)).thenReturn(internalResponse);

        DocumentInternalResponse response = documentInternalService.getDocumentById(documentId);

        assertEquals(internalResponse, response);
        verify(documentRepository).findById(documentId);
        verify(documentMapper).toInternalResponse(activeDocument);
    }

    @Test
    void uploadCompleted_whenNotFound_throwsDocumentNotFoundException() {
        when(documentRepository.findById(documentId)).thenReturn(java.util.Optional.empty());

        assertThrows(DocumentNotFoundException.class, () -> documentInternalService.uploadCompleted(documentId, "k", "m", 1L));

        verify(documentRepository).findById(documentId);
        verify(documentRepository, never()).save(any());
        verifyNoInteractions(documentMapper);
    }

    @Test
    void uploadCompleted_whenDeleted_throwsDocumentNotFoundException() {
        when(documentRepository.findById(documentId)).thenReturn(java.util.Optional.of(deletedDocument));

        assertThrows(DocumentNotFoundException.class, () -> documentInternalService.uploadCompleted(documentId, "k", "m", 1L));

        verify(documentRepository).findById(documentId);
        verify(documentRepository, never()).save(any());
        verifyNoInteractions(documentMapper);
    }

    @Test
    void uploadCompleted_updatesStorageAndStatus_andMapsResponse() {
        when(documentRepository.findById(documentId)).thenReturn(java.util.Optional.of(activeDocument));
        when(documentMapper.toInternalResponse(activeDocument)).thenReturn(internalResponse);

        DocumentInternalResponse response = documentInternalService.uploadCompleted(documentId, "newKey", "image/png", 42L);

        ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(docCaptor.capture());
        Document updated = docCaptor.getValue();

        assertEquals("newKey", updated.getStorageKey());
        assertEquals("image/png", updated.getMimeType());
        assertEquals(42L, updated.getSizeBytes());
        assertEquals(DocumentStatus.UPLOADED, updated.getStatus());
        assertEquals(internalResponse, response);
    }

    @Test
    void updateStatus_whenNotFound_throwsDocumentNotFoundException() {
        when(documentRepository.findById(documentId)).thenReturn(java.util.Optional.empty());

        assertThrows(DocumentNotFoundException.class, () -> documentInternalService.updateStatus(documentId, DocumentStatus.FAILED));

        verify(documentRepository).findById(documentId);
        verify(documentRepository, never()).save(any());
        verifyNoInteractions(documentMapper);
    }

    @Test
    void updateStatus_whenDeleted_throwsDocumentNotFoundException() {
        when(documentRepository.findById(documentId)).thenReturn(java.util.Optional.of(deletedDocument));

        assertThrows(DocumentNotFoundException.class, () -> documentInternalService.updateStatus(documentId, DocumentStatus.FAILED));

        verify(documentRepository).findById(documentId);
        verify(documentRepository, never()).save(any());
        verifyNoInteractions(documentMapper);
    }

    @Test
    void updateStatus_updatesStatusAndMapsResponse() {
        when(documentRepository.findById(documentId)).thenReturn(java.util.Optional.of(activeDocument));
        when(documentMapper.toInternalResponse(activeDocument)).thenReturn(internalResponse);

        DocumentInternalResponse response = documentInternalService.updateStatus(documentId, DocumentStatus.FAILED);

        ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(docCaptor.capture());
        Document updated = docCaptor.getValue();

        assertEquals(DocumentStatus.FAILED, updated.getStatus());
        assertEquals(internalResponse, response);
    }
}
