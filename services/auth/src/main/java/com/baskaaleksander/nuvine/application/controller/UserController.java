package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.RegisterRequest;
import com.baskaaleksander.nuvine.domain.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UUID> registerUser(
            @RequestBody @Valid RegisterRequest request
    ) {
        return ResponseEntity.status(CREATED).body(userService.register(request));
    }
}
