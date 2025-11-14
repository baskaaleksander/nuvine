package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.domain.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
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
}
