package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.EmailChangeRequest;
import com.baskaaleksander.nuvine.application.dto.EmailVerificationRequest;
import com.baskaaleksander.nuvine.domain.service.EmailVerificationService;
import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/email")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService service;

    @PostMapping("/request")
    @RateLimiting(
            name = "email_verify_request_limit",
            cacheKey = "@jwt.getSubject()",
            ratePerMethod = true
    )
    public ResponseEntity<Void> requestVerificationLink(
            HttpServletRequest httpRequest,
            @AuthenticationPrincipal Jwt jwt
    ) {
        service.requestVerificationLink(jwt.getClaimAsString("email"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify")
    @RateLimiting(
            name = "email_verify_limit",
            cacheKey = "@rateLimitHelper.getClientIP(#httpRequest)",
            ratePerMethod = true
    )
    public ResponseEntity<Void> verifyEmail(
            HttpServletRequest httpRequest,
            @RequestBody @Valid EmailVerificationRequest request
    ) {
        service.verifyEmail(request.token());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change")
    @RateLimiting(
            name = "email_change_limit",
            cacheKey = "@jwt.getSubject()",
            ratePerMethod = true
    )
    public ResponseEntity<Void> changeEmail(
            HttpServletRequest httpRequest,
            @RequestBody @Valid EmailChangeRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        service.changeEmail(jwt.getClaimAsString("email"), request.email(), request.password());
        return ResponseEntity.ok().build();
    }

}
