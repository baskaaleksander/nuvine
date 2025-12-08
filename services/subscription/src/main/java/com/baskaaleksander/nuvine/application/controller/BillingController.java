package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.*;
import com.baskaaleksander.nuvine.domain.service.BillingService;
import com.baskaaleksander.nuvine.domain.service.ModelPricingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;
    private final ModelPricingService modelPricingService;

    @GetMapping("/models/pricing")
    public ResponseEntity<List<ModelPricingResponse>> getModelPricing() {
        return ResponseEntity.ok(modelPricingService.getAllActivePricing());
    }

    @GetMapping("/workspaces/{workspaceId}/subscription")
    @PreAuthorize("@billingDataAccessEvaluator.canAccessBillingData(jwt.getSubject(), #workspaceId)")
    public ResponseEntity<SubscriptionStatusResponse> getSubscriptionStatus(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(billingService.getSubscriptionStatus(workspaceId));
    }

    @GetMapping("/workspaces/{workspaceId}/usage-logs")
    @PreAuthorize("@billingDataAccessEvaluator.canAccessBillingData(jwt.getSubject(), #workspaceId)")
    public ResponseEntity<PagedResponse<UsageLogResponse>> getUsageLogs(
            @PathVariable UUID workspaceId,
            @ModelAttribute UsageLogFilterRequest filter,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(billingService.getUsageLogs(workspaceId, filter));
    }

    @GetMapping("/workspaces/{workspaceId}/payments")
    @PreAuthorize("@billingDataAccessEvaluator.canAccessBillingData(jwt.getSubject(), #workspaceId)")
    public ResponseEntity<PagedResponse<PaymentResponse>> getPayments(
            @PathVariable UUID workspaceId,
            @ModelAttribute PaymentFilterRequest filter,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(billingService.getPayments(workspaceId, filter));
    }

    @GetMapping("/workspaces/{workspaceId}/usage/aggregations")
    @PreAuthorize("@billingDataAccessEvaluator.canAccessBillingData(jwt.getSubject(), #workspaceId)")
    public ResponseEntity<UsageAggregationResponse> getUsageAggregations(
            @PathVariable UUID workspaceId,
            @Valid @ModelAttribute UsageAggregationRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(billingService.getUsageAggregations(workspaceId, request));
    }
}
