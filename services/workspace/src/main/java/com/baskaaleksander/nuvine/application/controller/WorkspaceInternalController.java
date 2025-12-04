package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.WorkspaceInternalSubscriptionResponse;
import com.baskaaleksander.nuvine.domain.service.WorkspaceInternalService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/workspaces")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_INTERNAL_SERVICE')")
public class WorkspaceInternalController {

    private final WorkspaceInternalService workspaceInternalService;

    @GetMapping("/{workspaceId}")
    public WorkspaceInternalSubscriptionResponse getWorkspaceSubscription(@PathVariable UUID workspaceId) {
        return workspaceInternalService.getWorkspaceSubscription(workspaceId);
    }
}
