package com.baskaaleksander.nuvine.application.dto;

import java.math.BigDecimal;

public record CheckLimitResult(
        boolean approved,
        BigDecimal estimatedCost,
        BigDecimal usedCredits,
        BigDecimal reservedCredits,
        BigDecimal limitCredits
) {

    public static CheckLimitResult approved(
            BigDecimal estimatedCost,
            BigDecimal usedCredits,
            BigDecimal reservedCredits,
            BigDecimal limitCredits
    ) {
        return new CheckLimitResult(true, estimatedCost, usedCredits, reservedCredits, limitCredits);
    }

    public static CheckLimitResult rejected(
            BigDecimal usedCredits,
            BigDecimal reservedCredits,
            BigDecimal estimatedCost,
            BigDecimal limitCredits
    ) {
        return new CheckLimitResult(false, estimatedCost, usedCredits, reservedCredits, limitCredits);
    }
}
