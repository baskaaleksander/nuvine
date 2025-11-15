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

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(
            @RequestBody @Valid RegisterRequest request
    ) {
        return ResponseEntity.status(CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> loginUser(
            @RequestBody @Valid LoginRequest request
    ) {
        var tokenRes = authService.login(request);

        ResponseCookie cookie = ResponseCookie.from("refresh_token", tokenRes.refreshToken())
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
                                tokenRes.accessToken(),
                                tokenRes.expiresIn()
                        )
                );
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(
            @CookieValue("refresh_token") String refreshToken
    ) {
        var tokenRes = authService.refreshToken(refreshToken);

        ResponseCookie cookie = ResponseCookie.from("refresh_token", tokenRes.refreshToken())
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
                                tokenRes.accessToken(),
                                tokenRes.expiresIn()
                        )
                );
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> getMe(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(authService.getMe(jwt));
    }

    @PutMapping("/me")
    public ResponseEntity<MeResponse> updateMe(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid UpdateMeRequest request
    ) {
        return ResponseEntity.ok(authService.updateMe(jwt, request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue("refresh_token") String token
    ) {

        authService.logout(token);

        ResponseCookie clearCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(
            @CookieValue("refresh_token") String token
    ) {

        authService.logoutAll(token);

        ResponseCookie clearCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .build();
    }

    @GetMapping("/test")
    public Map<String, Object> test(@AuthenticationPrincipal Jwt jwt, Authentication auth) {
        return Map.of(
                "jwt_roles", jwt.getClaim("realm_access"),
                "authorities", auth.getAuthorities()
        );
    }
}
