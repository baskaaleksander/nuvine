package com.baskaaleksander.nuvine.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ReleaseReservationRequest(
        UUID workspaceId,
        BigDecimal amount
) {
}
