package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.domain.service.ProjectInternalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/projects")
@RequiredArgsConstructor
public class ProjectInternalController {

    private final ProjectInternalService projectInternalService;

    @GetMapping("/{projectId}/document-ids")
    public ResponseEntity<List<UUID>> getDocumentIdsInProject(@PathVariable UUID projectId) {
        return ResponseEntity.ok(projectInternalService.getDocumentIdsInProject(projectId));
    }
}
