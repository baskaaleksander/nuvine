package com.baskaaleksander.nuvine.application.mapper;

import com.baskaaleksander.nuvine.application.dto.DocumentInternalResponse;
import com.baskaaleksander.nuvine.application.dto.DocumentPublicResponse;
import com.baskaaleksander.nuvine.domain.model.Document;
import com.baskaaleksander.nuvine.domain.model.DocumentStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentMapperTest {

    private final DocumentMapper mapper = new DocumentMapper();

    @Test
    void toDocumentResponse_mapsAllFields() {
        UUID id = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        Instant createdAt = Instant.now();

        Document document = Document.builder()
                .id(id)
                .projectId(projectId)
                .workspaceId(workspaceId)
                .name("Doc")
                .status(DocumentStatus.PROCESSED)
                .createdBy(createdBy)
                .createdAt(createdAt)
                .build();

        DocumentPublicResponse response = mapper.toDocumentResponse(document);

        assertEquals(id, response.id());
        assertEquals(projectId, response.projectId());
        assertEquals(workspaceId, response.workspaceId());
        assertEquals("Doc", response.name());
        assertEquals(DocumentStatus.PROCESSED, response.status());
        assertEquals(createdBy, response.createdBy());
        assertEquals(createdAt, response.createdAt());
    }

    @Test
    void toInternalResponse_mapsAllFields() {
        UUID id = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        Instant createdAt = Instant.now();

        Document document = Document.builder()
                .id(id)
                .projectId(projectId)
                .workspaceId(workspaceId)
                .name("Doc")
                .status(DocumentStatus.UPLOADED)
                .storageKey("key")
                .mimeType("text/plain")
                .sizeBytes(123L)
                .createdBy(createdBy)
                .createdAt(createdAt)
                .build();

        DocumentInternalResponse response = mapper.toInternalResponse(document);

        assertEquals(id, response.id());
        assertEquals(projectId, response.projectId());
        assertEquals(workspaceId, response.workspaceId());
        assertEquals("Doc", response.name());
        assertEquals(DocumentStatus.UPLOADED, response.status());
        assertEquals("key", response.storageKey());
        assertEquals("text/plain", response.mimeType());
        assertEquals(123L, response.sizeBytes());
        assertEquals(createdBy, response.createdBy());
        assertEquals(createdAt, response.createdAt());
    }
}
