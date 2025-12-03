package com.baskaaleksander.subscription.domain.model;

public enum SubscriptionStatus {
    ACTIVE,
    TRIALING,
    PAST_DUE,
    CANCELED,
    INCOMPLETE,
    INCOMPLETE_EXPIRED,
    UNPAID
}
