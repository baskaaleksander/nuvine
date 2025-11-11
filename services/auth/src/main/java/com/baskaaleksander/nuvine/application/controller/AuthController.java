package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.*;
import com.baskaaleksander.nuvine.domain.service.AuthService;
import jakarta.validation.Valid;
import jakarta.ws.rs.core.HttpHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
        var tokenRes = service.login(request);

        ResponseCookie cookie = ResponseCookie.from("refresh_token", tokenRes.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(7 * 24 * 3600)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(
                    new TokenResponse(
                        tokenRes.getAccessToken(),
                        tokenRes.getExpiresIn()
                    )
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(
            @CookieValue("refresh_token") String refreshToken
    ) {
        var tokenRes = service.refreshToken(refreshToken);

        ResponseCookie cookie = ResponseCookie.from("refresh_token", tokenRes.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(7 * 24 * 3600)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(
                        new TokenResponse(
                                tokenRes.getAccessToken(),
                                tokenRes.getExpiresIn()
                        )
                );
    }

    @GetMapping("/test")
    public Map<String, Object> test(@AuthenticationPrincipal Jwt jwt, Authentication auth) {
        return Map.of(
                "jwt_roles", jwt.getClaim("realm_access"),
                "authorities", auth.getAuthorities()
        );
    }
}
