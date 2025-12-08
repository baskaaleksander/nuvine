package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.CheckLimitRequest;
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

    @PostMapping("/check-limit")
    public ResponseEntity<Boolean> checkLimit(
            @RequestBody @Valid CheckLimitRequest request
    ) {
        return ResponseEntity.ok(true);
    }
}
