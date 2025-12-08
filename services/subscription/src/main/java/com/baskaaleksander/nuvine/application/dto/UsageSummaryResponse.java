package com.baskaaleksander.nuvine.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UsageSummaryResponse(
        BigDecimal usedCredits,
        BigDecimal reservedCredits,
        long includedCredits,
        BigDecimal remainingCredits,
        LocalDate periodStart,
        LocalDate periodEnd
) {
}
