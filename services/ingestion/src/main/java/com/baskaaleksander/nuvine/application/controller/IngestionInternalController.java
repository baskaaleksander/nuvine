package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.IngestionJobConciseResponse;
import com.baskaaleksander.nuvine.application.dto.IngestionJobResponse;
import com.baskaaleksander.nuvine.application.dto.PagedResponse;
import com.baskaaleksander.nuvine.application.dto.PaginationRequest;
import com.baskaaleksander.nuvine.domain.model.IngestionStatus;
import com.baskaaleksander.nuvine.domain.service.IngestionCommandService;
import com.baskaaleksander.nuvine.domain.service.IngestionInternalService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/ingestion/jobs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('INTERNAL_SERVICE')")
public class IngestionInternalController {

    private final IngestionInternalService service;
    private final IngestionCommandService commandService;

    @GetMapping
    public ResponseEntity<PagedResponse<IngestionJobConciseResponse>> getAllJobs(
            @RequestParam(value = "workspaceId", required = false) String workspaceId,
            @RequestParam(value = "projectId", required = false) String projectId,
            @RequestParam(value = "status", required = false) IngestionStatus status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "id") String sortField,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        PaginationRequest request = new PaginationRequest(page, size, sortField, direction);

        return ResponseEntity.ok(service.getAllJobs(workspaceId, projectId, status, request));
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<IngestionJobResponse> getJobByDocumentId(
            @PathVariable UUID documentId
    ) {
        return ResponseEntity.ok(service.getJobByDocId(documentId));
    }

    @PostMapping("/{documentId}/start")
    public ResponseEntity<Void> startJob(
            @PathVariable String documentId
    ) {
        commandService.startIngestionJob(documentId);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{documentId}/retry")
    public ResponseEntity<Void> retryJob(
            @PathVariable String documentId
    ) {
        commandService.retryIngestionJob(documentId);
        return ResponseEntity.accepted().build();
    }

}
