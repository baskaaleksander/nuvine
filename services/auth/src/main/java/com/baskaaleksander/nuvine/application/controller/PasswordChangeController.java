package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.ForgotPasswordRequest;
import com.baskaaleksander.nuvine.domain.service.PasswordChangeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

}
