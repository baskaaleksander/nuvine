package com.baskaaleksander.nuvine.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record UsageAggregationResponse(
        List<TimeBasedUsageAggregation> timeBasedUsage,
        List<ModelBasedUsageAggregation> modelBasedUsage,
        BigDecimal totalCostCredits,
        long totalTokensIn,
        long totalTokensOut,
        long totalRequests
) {

    public record TimeBasedUsageAggregation(
            LocalDate period,
            long tokensIn,
            long tokensOut,
            BigDecimal costCredits,
            long requestCount
    ) {
    }

    public record ModelBasedUsageAggregation(
            String provider,
            String model,
            long tokensIn,
            long tokensOut,
            BigDecimal costCredits,
            long requestCount
    ) {
    }
}
