package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.WorkspaceInternalSubscriptionResponse;
import com.baskaaleksander.nuvine.domain.exception.WorkspaceMemberNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.WorkspaceNotFoundException;
import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceMemberRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkspaceInternalService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public WorkspaceInternalSubscriptionResponse getWorkspaceSubscription(UUID workspaceId) {
        var workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException("Workspace not found"));

        var workspaceOwnerId = workspaceMemberRepository.findOwnerUserIdByWorkspaceId(workspaceId)
                .orElseThrow(() -> new WorkspaceMemberNotFoundException("Workspace owner not found"));

        return new WorkspaceInternalSubscriptionResponse(
                workspace.getId(),
                workspace.getBillingTier(),
                workspace.getSubscriptionId(),
                workspaceOwnerId
        );
    }
}
