package com.baskaaleksander.nuvine.infrastructure.client;

import com.baskaaleksander.nuvine.application.dto.WorkspaceBillingTierUpdateRequest;
import com.baskaaleksander.nuvine.application.dto.WorkspaceInternalSubscriptionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceServiceCacheWrapper {

    private final WorkspaceServiceClient workspaceServiceClient;

    @Cacheable(cacheNames = "workspaces", key = "#workspaceId")
    public WorkspaceInternalSubscriptionResponse getWorkspaceSubscription(UUID workspaceId) {
        return workspaceServiceClient.getWorkspaceSubscription(workspaceId);
    }

    @CacheEvict(cacheNames = "workspaces", key = "#workspaceId")
    public void updateWorkspaceBillingTier(UUID workspaceId, WorkspaceBillingTierUpdateRequest request) {
        workspaceServiceClient.updateWorkspaceBillingTier(workspaceId, request);
    }
}
