package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.WorkspaceBillingTierUpdateRequest;
import com.baskaaleksander.nuvine.application.dto.WorkspaceInternalSubscriptionResponse;
import com.baskaaleksander.nuvine.domain.service.WorkspaceInternalService;
import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    @PatchMapping("/{workspaceId}/billing-tier")
    @RateLimiting(
            name = "workspace_billing_tier_update_internal_limit",
            cacheKey = "#workspaceId",
            ratePerMethod = true
    )
    public void updateWorkspaceBillingTier(
            @PathVariable UUID workspaceId,
            @RequestBody @Valid WorkspaceBillingTierUpdateRequest request
    ) {
        workspaceInternalService.updateWorkspaceBillingTier(workspaceId, request.billingTierCode(), request.stripeSubscriptionId());
    }
}
