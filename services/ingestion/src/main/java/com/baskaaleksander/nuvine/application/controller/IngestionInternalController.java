package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.PaginationRequest;
import com.baskaaleksander.nuvine.domain.model.IngestionStatus;
import com.baskaaleksander.nuvine.domain.service.IngestionInternalService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal/ingestion/jobs")
@RequiredArgsConstructor
//@PreAuthorize("hasRole('INTERNAL_SERVICE')")
public class IngestionInternalController {

    private final IngestionInternalService service;

    @GetMapping
    public ResponseEntity<?> getAllJobs(
            @RequestParam("workspaceId") String workspaceId,
            @RequestParam("projectId") String projectId,
            @RequestParam("status") IngestionStatus status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "id") String sortField,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        PaginationRequest request = new PaginationRequest(page, size, sortField, direction);

        return ResponseEntity.ok(service.getAllJobs(workspaceId, projectId, status, request));
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<?> getJobByDocumentId() {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{documentId}/start")
    public ResponseEntity<?> startJob() {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{documentId}/retry")
    public ResponseEntity<?> retryJob() {
        return ResponseEntity.ok().build();
    }

}
