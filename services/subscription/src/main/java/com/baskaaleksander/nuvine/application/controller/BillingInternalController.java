package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.CheckLimitRequest;
import com.baskaaleksander.nuvine.application.dto.CheckLimitResult;
import com.baskaaleksander.nuvine.application.dto.ReleaseReservationRequest;
import com.baskaaleksander.nuvine.domain.service.BillingInternalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/billing")
@RequiredArgsConstructor
@PreAuthorize("hasRole('INTERNAL_SERVICE')")
public class BillingInternalController {

    private final BillingInternalService billingInternalService;

    @PostMapping("/check-limit")
    public ResponseEntity<CheckLimitResult> checkLimit(
            @RequestBody @Valid CheckLimitRequest request
    ) {
        CheckLimitResult result = billingInternalService.checkAndReserveLimit(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/release-reservation")
    public ResponseEntity<Void> releaseReservation(
            @RequestBody @Valid ReleaseReservationRequest request
    ) {
        billingInternalService.releaseReservation(request.workspaceId(), request.amount());
        return ResponseEntity.ok().build();
    }
}
