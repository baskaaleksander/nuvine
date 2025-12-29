package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.*;
import com.baskaaleksander.nuvine.domain.service.ProjectService;
import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping("/api/v1/workspaces/{workspaceId}/projects")
    @PreAuthorize("@projectAccess.canManageProjectsInWorkspace(#workspaceId, #jwt.getSubject())")
    @RateLimiting(
            name = "create_project_limit",
            cacheKey = "@jwt.getSubject()",
            ratePerMethod = true
    )
    public ResponseEntity<ProjectResponse> createProject(
            @PathVariable UUID workspaceId,
            @RequestBody @Valid CreateProjectRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {

        return ResponseEntity.ok(projectService.createProject(workspaceId, request));
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/projects")
    @PreAuthorize("@projectAccess.canGetProjectsInWorkspace(#workspaceId, #jwt.getSubject())")
    public ResponseEntity<PagedResponse<ProjectResponse>> getProjects(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "id") String sortField,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        PaginationRequest request = new PaginationRequest(page, size, sortField, direction);
        return ResponseEntity.ok(projectService.getProjects(workspaceId, request));
    }

    @GetMapping("/api/v1/projects/{projectId}")
    @PreAuthorize("@projectAccess.canGetProject(#projectId, #jwt.getSubject())")
    public ResponseEntity<ProjectDetailedResponse> getProjectById(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        ProjectDetailedResponse project = projectService.getProjectById(projectId);
        return ResponseEntity.ok(project);
    }

    @PatchMapping("/api/v1/projects/{projectId}")
    @PreAuthorize("@projectAccess.canManageProject(#projectId, #jwt.getSubject())")
    @RateLimiting(
            name = "update_project_limit",
            cacheKey = "@jwt.getSubject()",
            ratePerMethod = true
    )
    public ResponseEntity<Void> updateProject(
            @PathVariable UUID projectId,
            @RequestBody @Valid UpdateProjectRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        projectService.updateProject(projectId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/v1/projects/{projectId}")
    @PreAuthorize("@projectAccess.canManageProject(#projectId, #jwt.getSubject())")
    @RateLimiting(
            name = "delete_project_limit",
            cacheKey = "@jwt.getSubject()",
            ratePerMethod = true
    )
    public ResponseEntity<Void> deleteProject(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        projectService.deleteProject(projectId);
        return ResponseEntity.noContent().build();
    }
}
