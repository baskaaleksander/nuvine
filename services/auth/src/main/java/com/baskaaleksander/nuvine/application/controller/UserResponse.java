package com.baskaaleksander.nuvine.application.controller;

import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        List<String> roles
) {
}
