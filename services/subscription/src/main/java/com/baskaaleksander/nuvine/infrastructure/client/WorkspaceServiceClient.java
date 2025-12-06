package com.baskaaleksander.nuvine.infrastructure.client;

import com.baskaaleksander.nuvine.application.dto.WorkspaceBillingTierUpdateRequest;
import com.baskaaleksander.nuvine.application.dto.WorkspaceInternalSubscriptionResponse;
import com.baskaaleksander.nuvine.infrastructure.config.InternalFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(
        name = "workspace-service",
        url = "${application.config.api-base-url}",
        contextId = "workspaceServiceClient",
        configuration = InternalFeignConfig.class
)
public interface WorkspaceServiceClient {

    @GetMapping("/internal/workspaces/{workspaceId}")
    WorkspaceInternalSubscriptionResponse getWorkspaceSubscription(@PathVariable UUID workspaceId);

    @PatchMapping("/internal/workspaces/{workspaceId}/billing-tier")
    void updateWorkspaceBillingTier(@PathVariable UUID workspaceId, @RequestBody WorkspaceBillingTierUpdateRequest request);
}
