package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.InviteWorkspaceMemberRequest;
import com.baskaaleksander.nuvine.application.dto.WorkspaceMemberResponse;
import com.baskaaleksander.nuvine.application.dto.WorkspaceMemberRoleRequest;
import com.baskaaleksander.nuvine.application.dto.WorkspaceMembersResponse;
import com.baskaaleksander.nuvine.domain.service.WorkspaceMemberService;
import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

//    @PostMapping
//    @PreAuthorize("@workspaceAccess.canEditWorkspace(#workspaceId, #jwt.getSubject())")
//    public ResponseEntity<Void> addWorkspaceMember(
//            @PathVariable UUID workspaceId,
//            @RequestBody @Valid WorkspaceMemberRequest request,
//            @AuthenticationPrincipal Jwt jwt
//    ) {
//        workspaceMemberService.addWorkspaceMember(workspaceId, request.userId(), request.role());
//        return ResponseEntity.ok().build();
//    }

    @GetMapping("/me")
    @PreAuthorize("@workspaceAccess.canViewWorkspace(#workspaceId, #jwt.getSubject())")
    public ResponseEntity<WorkspaceMemberResponse> getWorkspaceMember(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(workspaceMemberService.getWorkspaceMember(workspaceId, UUID.fromString(jwt.getSubject())));
    }

    @DeleteMapping("/me")
    @RateLimiting(
            name = "remove_workspace_member_limit",
            cacheKey = "@jwt.getSubject()",
            ratePerMethod = true
    )
    public ResponseEntity<Void> removeWorkspaceMemberSelf(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        workspaceMemberService.removeWorkspaceMember(workspaceId, UUID.fromString(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{userId}")
    @PreAuthorize("@workspaceAccess.canEditWorkspace(#workspaceId, #jwt.getSubject())")
    @RateLimiting(
            name = "update_workspace_member_role_limit",
            cacheKey = "@jwt.getSubject()",
            ratePerMethod = true
    )
    public ResponseEntity<Void> updateWorkspaceMemberRole(
            @PathVariable UUID workspaceId,
            @PathVariable UUID userId,
            @RequestBody @Valid WorkspaceMemberRoleRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        workspaceMemberService.updateWorkspaceMemberRole(workspaceId, userId, request.role());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("@workspaceAccess.canEditWorkspace(#workspaceId, #jwt.getSubject())")
    @RateLimiting(
            name = "remove_workspace_member_limit",
            cacheKey = "@jwt.getSubject()",
            ratePerMethod = true
    )
    public ResponseEntity<Void> removeWorkspaceMember(
            @PathVariable UUID workspaceId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        workspaceMemberService.removeWorkspaceMember(workspaceId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/invite")
    @PreAuthorize("@workspaceAccess.canEditWorkspace(#workspaceId, #jwt.getSubject())")
    @ResponseStatus(HttpStatus.CREATED)
    @RateLimiting(
            name = "invite_workspace_member_limit",
            cacheKey = "@jwt.getSubject()",
            ratePerMethod = true
    )
    public void inviteWorkspaceMember(
            @PathVariable UUID workspaceId,
            @RequestBody @Valid InviteWorkspaceMemberRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        workspaceMemberService.inviteWorkspaceMember(workspaceId, request.email(), request.role());
    }
}
