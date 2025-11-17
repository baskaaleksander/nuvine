package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.*;
import com.baskaaleksander.nuvine.domain.service.WorkspaceService;
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
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {
    private final WorkspaceService workspaceService;

    @PostMapping
    public WorkspaceCreateResponse createWorkspace(
            @RequestBody @Valid WorkspaceCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return workspaceService.createWorkspace(request.name(), UUID.fromString(jwt.getSubject()));
    }

    @GetMapping
    public PagedResponse<WorkspaceResponse> getWorkspaces(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "id") String sortField,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        PaginationRequest request = new PaginationRequest(page, size, sortField, direction);

        return workspaceService.getWorkspaces(UUID.fromString(jwt.getSubject()), request);
    }

    @GetMapping("/{workspaceId}")
    @PreAuthorize("@workspaceAccess.canViewWorkspace(#workspaceId, #jwt.getSubject())")
    public WorkspaceResponseWithStats getWorkspace(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return workspaceService.getWorkspace(workspaceId);
    }

    @PatchMapping("/{workspaceId}")
    @PreAuthorize("@workspaceAccess.canEditWorkspace(#workspaceId, #jwt.getSubject())")
    public ResponseEntity<Void> updateWorkspace(
            @PathVariable UUID workspaceId,
            @RequestBody @Valid WorkspaceUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        workspaceService.updateWorkspace(workspaceId, request.name(), UUID.fromString(jwt.getSubject()));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{workspaceId}/me")
    @PreAuthorize("@workspaceAccess.canViewWorkspace(#workspaceId, #jwt.getSubject())")
    public WorkspaceMemberResponse getSelfWorkspaceMember(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return workspaceService.getSelfWorkspaceMember(workspaceId, UUID.fromString(jwt.getSubject()));
    }

    @DeleteMapping("/{workspaceId}")
    @PreAuthorize("@workspaceAccess.canEditWorkspace(#workspaceId, #jwt.getSubject())")
    public ResponseEntity<Void> deleteWorkspace(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        workspaceService.deleteWorkspace(workspaceId);
        return ResponseEntity.noContent().build();
    }
}
