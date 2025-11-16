package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.WorkspaceMemberRequest;
import com.baskaaleksander.nuvine.application.dto.WorkspaceMembersResponse;
import com.baskaaleksander.nuvine.domain.service.WorkspaceMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/members")
@RequiredArgsConstructor
public class WorkspaceMemberController {

    private final WorkspaceMemberService workspaceMemberService;

    @GetMapping
    @PreAuthorize("@workspaceAccess.canViewWorkspace(#workspaceId, #jwt.getSubject())")
    public WorkspaceMembersResponse getWorkspaceMembers(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return workspaceMemberService.getWorkspaceMembers(workspaceId);
    }

    @PostMapping
    @PreAuthorize("@workspaceAccess.canEditWorkspace(#workspaceId, #jwt.getSubject())")
    public ResponseEntity<Void> addWorkspaceMember(
            @PathVariable UUID workspaceId,
            @RequestBody @Valid WorkspaceMemberRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        workspaceMemberService.addWorkspaceMember(workspaceId, request.userId(), request.role());
        return ResponseEntity.ok().build();
    }
}
