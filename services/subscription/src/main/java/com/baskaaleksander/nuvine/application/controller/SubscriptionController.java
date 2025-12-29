package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.CustomerPortalSessionRequest;
import com.baskaaleksander.nuvine.application.dto.CustomerPortalSessionResponse;
import com.baskaaleksander.nuvine.application.dto.PaymentSessionRequest;
import com.baskaaleksander.nuvine.application.dto.PaymentSessionResponse;
import com.baskaaleksander.nuvine.domain.service.SubscriptionService;
import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/payment-session")
    @RateLimiting(
            name = "payment_session_limit",
            cacheKey = "@jwt.getSubject()",
            ratePerMethod = true
    )
    public ResponseEntity<PaymentSessionResponse> createPaymentSession(
            @RequestBody @Valid PaymentSessionRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                subscriptionService.createPaymentSession(
                        request.workspaceId(),
                        request.planId(),
                        request.intent(),
                        UUID.fromString(jwt.getSubject())
                )
        );
    }

    @PostMapping("/customer-portal-session")
    @RateLimiting(
            name = "customer_portal_limit",
            cacheKey = "@jwt.getSubject()",
            ratePerMethod = true
    )
    public ResponseEntity<CustomerPortalSessionResponse> createCustomerPortalSession(
            @RequestBody @Valid CustomerPortalSessionRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                subscriptionService.createCustomerPortalSession(
                        request.workspaceId(),
                        UUID.fromString(jwt.getSubject())
                )
        );
    }
}
