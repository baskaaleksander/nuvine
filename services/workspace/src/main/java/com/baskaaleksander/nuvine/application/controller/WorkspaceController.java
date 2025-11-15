package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.WorkspaceCreateRequest;
import com.baskaaleksander.nuvine.application.dto.WorkspaceCreateResponse;
import com.baskaaleksander.nuvine.application.dto.WorkspaceResponse;
import com.baskaaleksander.nuvine.domain.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    public List<WorkspaceResponse> getWorkspaces(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return workspaceService.getWorkspaces(UUID.fromString(jwt.getSubject()));
    }
}
