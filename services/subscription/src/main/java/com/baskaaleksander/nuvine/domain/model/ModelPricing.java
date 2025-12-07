package com.baskaaleksander.nuvine.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

@Embeddable
public class ModelPricing {
    @Column(nullable = false, precision = 12, scale = 8)
    private BigDecimal inputPricePer1MTokens;

    @Column(nullable = false, precision = 12, scale = 8)
    private BigDecimal outputPricePer1MTokens;

    @Column(precision = 12, scale = 8)
    private BigDecimal imagePricePer1K;

    @Column(precision = 12, scale = 8)
    private BigDecimal audioInputPricePer1MTokens;  // nullable

    @Column(precision = 12, scale = 8)
    private BigDecimal audioOutputPricePer1MTokens;  // nullable

    @Column(length = 3, nullable = false)
    private String currency = "USD";
}
