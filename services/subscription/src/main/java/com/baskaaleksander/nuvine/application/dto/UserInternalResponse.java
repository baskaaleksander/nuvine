package com.baskaaleksander.nuvine.application.dto;

import java.util.UUID;

public record UserInternalResponse(
        UUID id,
        String firstName,
        String lastName,
        String email
) {
}
