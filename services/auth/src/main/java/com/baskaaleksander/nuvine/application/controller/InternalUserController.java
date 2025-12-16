package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.UserInternalResponse;
import com.baskaaleksander.nuvine.domain.service.UserService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/auth/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    @GetMapping("/{id}")
    public ResponseEntity<UserInternalResponse> checkInternalUser(@PathVariable UUID id) {

        return ResponseEntity.ok(userService.checkInternalUser(id));
    }

    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    @GetMapping("/email")
    public ResponseEntity<UserInternalResponse> checkInternalUser(@RequestParam @Email @NotBlank String email) {

        return ResponseEntity.ok(userService.checkInternalUserByEmail(email));
    }
}
