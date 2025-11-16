package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.domain.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    @GetMapping("/{id}")
    public ResponseEntity<Void> checkInternalUser(@PathVariable UUID id) {
        userService.validateUserExists(id);
        return ResponseEntity.ok().build();
    }
}
