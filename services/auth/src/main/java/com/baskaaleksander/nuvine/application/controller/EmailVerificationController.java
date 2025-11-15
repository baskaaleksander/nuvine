package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.EmailChangeRequest;
import com.baskaaleksander.nuvine.application.dto.EmailVerificationRequest;
import com.baskaaleksander.nuvine.domain.service.EmailVerificationService;
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
    public ResponseEntity<Void> requestVerificationLink(@AuthenticationPrincipal Jwt jwt) {
        service.requestVerificationLink(jwt.getClaimAsString("email"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify")
    public ResponseEntity<Void> verifyEmail(@RequestBody @Valid EmailVerificationRequest request) {
        service.verifyEmail(request.token());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change")
    public ResponseEntity<Void> changeEmail(
            @RequestBody @Valid EmailChangeRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        service.changeEmail(jwt.getClaimAsString("email"), request.email(), request.password());
        return ResponseEntity.ok().build();
    }

}
