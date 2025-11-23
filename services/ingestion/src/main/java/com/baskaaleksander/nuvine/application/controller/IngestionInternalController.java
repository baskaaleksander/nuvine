package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.domain.model.IngestionStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal/ingestion/jobs")
@PreAuthorize("hasRole('INTERNAL_SERVICE')")
public class IngestionInternalController {

    @GetMapping
    public ResponseEntity<?> getAllJobs(
            @RequestParam("workspaceId") String workspaceId,
            @RequestParam("projectId") String projectId,
            @RequestParam("status") IngestionStatus status
    ) {
        return ResponseEntity.ok().build();
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
