package com.baskaaleksander.nuvine.application.dto;

import com.baskaaleksander.nuvine.domain.model.ModelPricing;

public record ModelPricingResponse(
        String provider,
        String model,
        String displayName,
        ModelPricing pricing
) {
}
