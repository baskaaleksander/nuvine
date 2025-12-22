package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.IngestionJobConciseResponse;
import com.baskaaleksander.nuvine.application.dto.IngestionJobResponse;
import com.baskaaleksander.nuvine.application.dto.PagedResponse;
import com.baskaaleksander.nuvine.application.dto.PaginationRequest;
import com.baskaaleksander.nuvine.application.mapper.IngestionJobMapper;
import com.baskaaleksander.nuvine.domain.exception.IngestionJobNotFoundException;
import com.baskaaleksander.nuvine.domain.model.IngestionJob;
import com.baskaaleksander.nuvine.domain.model.IngestionStage;
import com.baskaaleksander.nuvine.domain.model.IngestionStatus;
import com.baskaaleksander.nuvine.infrastructure.repository.IngestionJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngestionInternalServiceTest {

    @Mock
    private IngestionJobRepository ingestionJobRepository;

    @Mock
    private IngestionJobMapper mapper;

    @InjectMocks
    private IngestionInternalService ingestionInternalService;

    private UUID documentId;
    private UUID workspaceId;
    private UUID projectId;
    private IngestionJob job;
    private IngestionJobResponse jobResponse;
    private IngestionJobConciseResponse conciseResponse;
    private PaginationRequest paginationRequest;

    @BeforeEach
    void setUp() {
        documentId = UUID.randomUUID();
        workspaceId = UUID.randomUUID();
        projectId = UUID.randomUUID();

        job = IngestionJob.builder()
                .id(UUID.randomUUID())
                .documentId(documentId)
                .workspaceId(workspaceId)
                .projectId(projectId)
                .storageKey("workspaces/" + workspaceId + "/documents/" + documentId + ".pdf")
                .mimeType("application/pdf")
                .status(IngestionStatus.PROCESSING)
                .stage(IngestionStage.FETCH)
                .retryCount(0)
                .version(0L)
                .build();

        jobResponse = new IngestionJobResponse(
                job.getId(),
                documentId,
                workspaceId,
                projectId,
                IngestionStatus.PROCESSING,
                IngestionStage.FETCH,
                0,
                null,
                Instant.now(),
                Instant.now()
        );

        conciseResponse = new IngestionJobConciseResponse(
                documentId,
                IngestionStatus.PROCESSING,
                IngestionStage.FETCH,
                null,
                Instant.now()
        );

        paginationRequest = new PaginationRequest();
        paginationRequest.setPage(1);
        paginationRequest.setSize(10);
        paginationRequest.setSortField("createdAt");
        paginationRequest.setDirection(Sort.Direction.DESC);
    }

    @Test
    void getAllJobs_noFilters_returnsPaginatedJobs() {
        Page<IngestionJob> page = new PageImpl<>(List.of(job));
        when(ingestionJobRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(mapper.toConciseResponse(job)).thenReturn(conciseResponse);

        PagedResponse<IngestionJobConciseResponse> result = ingestionInternalService.getAllJobs(
                null, null, null, paginationRequest);

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertTrue(result.content().contains(conciseResponse));
    }

    @Test
    void getAllJobs_withWorkspaceFilter_filtersCorrectly() {
        Page<IngestionJob> page = new PageImpl<>(List.of(job));
        when(ingestionJobRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(mapper.toConciseResponse(job)).thenReturn(conciseResponse);

        PagedResponse<IngestionJobConciseResponse> result = ingestionInternalService.getAllJobs(
                workspaceId.toString(), null, null, paginationRequest);

        assertNotNull(result);
        verify(ingestionJobRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getAllJobs_withProjectFilter_filtersCorrectly() {
        Page<IngestionJob> page = new PageImpl<>(List.of(job));
        when(ingestionJobRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(mapper.toConciseResponse(job)).thenReturn(conciseResponse);

        PagedResponse<IngestionJobConciseResponse> result = ingestionInternalService.getAllJobs(
                null, projectId.toString(), null, paginationRequest);

        assertNotNull(result);
        verify(ingestionJobRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getAllJobs_withStatusFilter_filtersCorrectly() {
        Page<IngestionJob> page = new PageImpl<>(List.of(job));
        when(ingestionJobRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(mapper.toConciseResponse(job)).thenReturn(conciseResponse);

        PagedResponse<IngestionJobConciseResponse> result = ingestionInternalService.getAllJobs(
                null, null, IngestionStatus.PROCESSING, paginationRequest);

        assertNotNull(result);
        verify(ingestionJobRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getAllJobs_withAllFilters_combinesFilters() {
        Page<IngestionJob> page = new PageImpl<>(List.of(job));
        when(ingestionJobRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(mapper.toConciseResponse(job)).thenReturn(conciseResponse);

        PagedResponse<IngestionJobConciseResponse> result = ingestionInternalService.getAllJobs(
                workspaceId.toString(), projectId.toString(), IngestionStatus.PROCESSING, paginationRequest);

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertTrue(result.content().contains(conciseResponse));
    }

    @Test
    void getAllJobs_emptyResult_returnsEmptyPage() {
        Page<IngestionJob> emptyPage = new PageImpl<>(List.of());
        when(ingestionJobRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        PagedResponse<IngestionJobConciseResponse> result = ingestionInternalService.getAllJobs(
                null, null, null, paginationRequest);

        assertNotNull(result);
        assertTrue(result.content().isEmpty());
    }

    @Test
    void getJobByDocId_existingJob_returnsJob() {
        when(ingestionJobRepository.findByDocumentId(documentId)).thenReturn(Optional.of(job));
        when(mapper.toResponse(job)).thenReturn(jobResponse);

        IngestionJobResponse result = ingestionInternalService.getJobByDocId(documentId);

        assertEquals(jobResponse, result);
        verify(ingestionJobRepository).findByDocumentId(documentId);
        verify(mapper).toResponse(job);
    }

    @Test
    void getJobByDocId_nonExistingJob_throwsIngestionJobNotFoundException() {
        when(ingestionJobRepository.findByDocumentId(documentId)).thenReturn(Optional.empty());

        assertThrows(IngestionJobNotFoundException.class,
                () -> ingestionInternalService.getJobByDocId(documentId));

        verify(mapper, never()).toResponse(any());
    }
}
