package com.baskaaleksander.nuvine.application.mapper;

import com.baskaaleksander.nuvine.application.dto.IngestionJobConciseResponse;
import com.baskaaleksander.nuvine.application.dto.IngestionJobResponse;
import com.baskaaleksander.nuvine.domain.model.IngestionJob;
import com.baskaaleksander.nuvine.domain.model.IngestionStage;
import com.baskaaleksander.nuvine.domain.model.IngestionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class IngestionJobMapperTest {

    private IngestionJobMapper mapper;
    private IngestionJob job;
    private UUID jobId;
    private UUID documentId;
    private UUID workspaceId;
    private UUID projectId;
    private Instant createdAt;
    private Instant updatedAt;

    @BeforeEach
    void setUp() {
        mapper = new IngestionJobMapper();

        jobId = UUID.randomUUID();
        documentId = UUID.randomUUID();
        workspaceId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        createdAt = Instant.now().minusSeconds(3600);
        updatedAt = Instant.now();

        job = IngestionJob.builder()
                .id(jobId)
                .documentId(documentId)
                .workspaceId(workspaceId)
                .projectId(projectId)
                .storageKey("workspaces/" + workspaceId + "/documents/" + documentId + ".pdf")
                .mimeType("application/pdf")
                .status(IngestionStatus.PROCESSING)
                .stage(IngestionStage.PARSE)
                .retryCount(2)
                .lastError("Previous error")
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .version(0L)
                .build();
    }

    @Test
    void toResponse_validJob_mapsAllFields() {
        IngestionJobResponse response = mapper.toResponse(job);

        assertNotNull(response);
        assertEquals(jobId, response.id());
        assertEquals(documentId, response.documentId());
        assertEquals(workspaceId, response.workspaceId());
        assertEquals(projectId, response.projectId());
        assertEquals(IngestionStatus.PROCESSING, response.status());
        assertEquals(IngestionStage.PARSE, response.stage());
        assertEquals(2, response.retryCount());
        assertEquals("Previous error", response.lastError());
        assertEquals(updatedAt, response.updatedAt());
        assertEquals(createdAt, response.createdAt());
    }

    @Test
    void toConciseResponse_validJob_mapsCorrectFields() {
        IngestionJobConciseResponse response = mapper.toConciseResponse(job);

        assertNotNull(response);
        assertEquals(documentId, response.documentId());
        assertEquals(IngestionStatus.PROCESSING, response.status());
        assertEquals(IngestionStage.PARSE, response.stage());
        assertEquals("Previous error", response.lastError());
        assertEquals(updatedAt, response.updatedAt());
    }

    @Test
    void toResponse_jobWithNullError_mapsNullError() {
        job.setLastError(null);

        IngestionJobResponse response = mapper.toResponse(job);

        assertNull(response.lastError());
    }

    @Test
    void toConciseResponse_jobWithNullError_mapsNullError() {
        job.setLastError(null);

        IngestionJobConciseResponse response = mapper.toConciseResponse(job);

        assertNull(response.lastError());
    }
}
