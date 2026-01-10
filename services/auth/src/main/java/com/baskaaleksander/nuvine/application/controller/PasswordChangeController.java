package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.CheckTokenRequest;
import com.baskaaleksander.nuvine.application.dto.ForgotPasswordRequest;
import com.baskaaleksander.nuvine.application.dto.PasswordChangeRequest;
import com.baskaaleksander.nuvine.application.dto.PasswordResetRequest;
import com.baskaaleksander.nuvine.domain.service.PasswordChangeService;
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
@RequestMapping("/api/v1/auth/password")
@RequiredArgsConstructor
public class PasswordChangeController {

    private final PasswordChangeService service;

    @PostMapping("/forgot")
    @RateLimiting(
            name = "forgot_password_limit",
            cacheKey = "@rateLimitHelper.getClientIP(#httpRequest)",
            ratePerMethod = true
    )
    public ResponseEntity<Void> forgotPassword(
            HttpServletRequest httpRequest,
            @RequestBody @Valid ForgotPasswordRequest request
    ) {
        service.requestPasswordReset(request.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset")
    @RateLimiting(
            name = "reset_password_limit",
            cacheKey = "@rateLimitHelper.getClientIP(#httpRequest)",
            ratePerMethod = true
    )
    public ResponseEntity<Void> resetPassword(
            HttpServletRequest httpRequest,
            @RequestBody @Valid PasswordResetRequest request
    ) {
        service.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change")
    @RateLimiting(
            name = "password_change_limit",
            cacheKey = "#jwt.getSubject()",
            ratePerMethod = true
    )
    public ResponseEntity<Void> changePassword(
            HttpServletRequest httpRequest,
            @RequestBody @Valid PasswordChangeRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        service.changePassword(request, jwt.getClaimAsString("email"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/check-token")
    @RateLimiting(
            name = "check_password_reset_token_limit",
            cacheKey = "@rateLimitHelper.getClientIP(#httpRequest)",
            ratePerMethod = true
    )
    public ResponseEntity<Void> checkToken(
            HttpServletRequest httpRequest,
            @RequestBody @Valid CheckTokenRequest request
    ) {
        service.checkToken(request.token());
        return ResponseEntity.ok().build();
    }
}
