package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.CreateProjectRequest;
import com.baskaaleksander.nuvine.domain.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @PreAuthorize("@projectAccess.canCreateProject(#workspaceId, #jwt.getSubject())")
    public ResponseEntity<Void> createProject(
            @PathVariable UUID workspaceId,
            @RequestBody @Valid CreateProjectRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        projectService.createProject(workspaceId, request);
        return ResponseEntity.ok().build();
    }
}
