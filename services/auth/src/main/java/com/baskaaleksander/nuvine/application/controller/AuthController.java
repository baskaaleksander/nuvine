package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.LoginRequest;
import com.baskaaleksander.nuvine.application.dto.RegisterRequest;
import com.baskaaleksander.nuvine.application.dto.TokenResponse;
import com.baskaaleksander.nuvine.application.dto.UserResponse;
import com.baskaaleksander.nuvine.domain.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(
            @RequestBody @Valid RegisterRequest request
    ) {
        return ResponseEntity.status(CREATED).body(service.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> loginUser(
            @RequestBody @Valid LoginRequest request
    ) {
        return ResponseEntity.ok(service.login(request));
    }
}
