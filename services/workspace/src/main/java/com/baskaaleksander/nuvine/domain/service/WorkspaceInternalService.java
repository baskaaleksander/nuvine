package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.WorkspaceInternalSubscriptionResponse;
import com.baskaaleksander.nuvine.domain.exception.WorkspaceMemberNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.WorkspaceNotFoundException;
import com.baskaaleksander.nuvine.domain.model.BillingTier;
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
        log.info("GET_WORKSPACE_INTERNAL START workspaceId={}", workspaceId);
        var workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> {
                    log.info("GET_WORKSPACE_INTERNAL FAILED reason=workspace_not_found workspaceId={}", workspaceId);
                    return new WorkspaceNotFoundException("Workspace not found");
                });

        var workspaceOwnerId = workspaceMemberRepository.findOwnerUserIdByWorkspaceId(workspaceId)
                .orElseThrow(() -> {
                    log.info("GET_WORKSPACE_INTERNAL FAILED reason=workspace_owner_not_found workspaceId={}", workspaceId);
                    return new WorkspaceMemberNotFoundException("Workspace owner not found");
                });

        log.info("GET_WORKSPACE_INTERNAL END workspaceId={}", workspaceId);

        return new WorkspaceInternalSubscriptionResponse(
                workspace.getId(),
                workspace.getBillingTier(),
                workspace.getSubscriptionId(),
                workspaceOwnerId
        );
    }

    public void updateWorkspaceBillingTier(UUID workspaceId, String billingTierCode) {
        var workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> {
                    log.info("UPDATE_WORKSPACE_BILLING_TIER FAILED reason=workspace_not_found workspaceId={}", workspaceId);
                    return new WorkspaceNotFoundException("Workspace not found");
                });

        workspace.setBillingTier(BillingTier.fromString(billingTierCode));
        workspaceRepository.save(workspace);
    }
}
