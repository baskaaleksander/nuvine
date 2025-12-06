package com.baskaaleksander.nuvine.domain.model;

public enum BillingTier {
    FREE,
    PRO,
    MAX;

    public static BillingTier fromString(String name) {
        return BillingTier.valueOf(name.toUpperCase());
    }
}
