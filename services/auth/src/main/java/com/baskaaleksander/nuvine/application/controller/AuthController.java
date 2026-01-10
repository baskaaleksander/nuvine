package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.*;
import com.baskaaleksander.nuvine.domain.service.AuthService;
import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.ws.rs.core.HttpHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @RateLimiting(
            name = "register_limit",
            cacheKey = "@rateLimitHelper.getClientIP(#httpRequest)",
            ratePerMethod = true
    )
    public ResponseEntity<UserResponse> registerUser(
            HttpServletRequest httpRequest,
            @RequestBody @Valid RegisterRequest request
    ) {
        return ResponseEntity.status(CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @RateLimiting(
            name = "login_limit",
            cacheKey = "@rateLimitHelper.getClientIP(#httpRequest)",
            ratePerMethod = true
    )
    public ResponseEntity<TokenResponse> loginUser(
            HttpServletRequest httpRequest,
            @RequestBody @Valid LoginRequest request
    ) {
        var tokenRes = authService.login(request);

        ResponseCookie cookie = ResponseCookie.from("refresh_token", tokenRes.refreshToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
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
    @RateLimiting(
            name = "refresh_token_limit",
            cacheKey = "@rateLimitHelper.getClientIP(#httpRequest)",
            ratePerMethod = true
    )
    public ResponseEntity<TokenResponse> refreshToken(
            HttpServletRequest httpRequest,
            @CookieValue("refresh_token") String refreshToken
    ) {
        var tokenRes = authService.refreshToken(refreshToken);

        ResponseCookie cookie = ResponseCookie.from("refresh_token", tokenRes.refreshToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
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

    @PatchMapping("/me")
    @RateLimiting(
            name = "profile_update_limit",
            cacheKey = "#jwt.getSubject()",
            ratePerMethod = true
    )
    public ResponseEntity<MeResponse> updateMe(
            HttpServletRequest httpRequest,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid UpdateMeRequest request
    ) {
        return ResponseEntity.ok(authService.updateMe(jwt, request));
    }

    @PostMapping("/logout")
    @RateLimiting(
            name = "logout_limit",
            cacheKey = "@rateLimitHelper.getClientIP(#httpRequest)",
            ratePerMethod = true
    )
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest,
            @CookieValue("refresh_token") String token
    ) {

        authService.logout(token, jwt);

        ResponseCookie clearCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .build();
    }

    @PostMapping("/logout-all")
    @RateLimiting(
            name = "logout_limit",
            cacheKey = "@rateLimitHelper.getClientIP(#httpRequest)",
            ratePerMethod = true
    )
    public ResponseEntity<Void> logoutAll(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest,
            @CookieValue("refresh_token") String token
    ) {

        authService.logoutAll(token, jwt);

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

}
