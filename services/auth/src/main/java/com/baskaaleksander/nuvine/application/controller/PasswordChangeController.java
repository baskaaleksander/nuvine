package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.CheckTokenRequest;
import com.baskaaleksander.nuvine.application.dto.ForgotPasswordRequest;
import com.baskaaleksander.nuvine.application.dto.PasswordChangeRequest;
import com.baskaaleksander.nuvine.application.dto.PasswordResetRequest;
import com.baskaaleksander.nuvine.domain.service.PasswordChangeService;
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
    public ResponseEntity<Void> forgotPassword(
            @RequestBody @Valid ForgotPasswordRequest request
    ) {
        service.requestPasswordReset(request.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> resetPassword(
            @RequestBody @Valid PasswordResetRequest request
    ) {
        service.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change")
    public ResponseEntity<Void> changePassword(
            @RequestBody @Valid PasswordChangeRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        service.changePassword(request, jwt.getClaimAsString("email"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/check-token")
    public ResponseEntity<Void> checkToken(
            @RequestBody @Valid CheckTokenRequest request
    ) {
        service.checkToken(request.token());
        return ResponseEntity.ok().build();
    }
}
