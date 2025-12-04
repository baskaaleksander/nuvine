package com.baskaaleksander.subscription.application.controller;

import com.baskaaleksander.subscription.application.dto.PaymentSessionRequest;
import com.baskaaleksander.subscription.application.dto.PaymentSessionResponse;
import com.baskaaleksander.subscription.domain.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/payment-session")
    public ResponseEntity<PaymentSessionResponse> createPaymentSession(
            @RequestBody @Valid PaymentSessionRequest request
    ) {
        return ResponseEntity.ok(subscriptionService.createPaymentSession(request.workspaceId(), request.planId(), request.intent(), request.userId(), request.email()));
    }
}
