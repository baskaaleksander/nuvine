package com.baskaaleksander.nuvine.domain.security;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("billingDataAccessEvaluator")
public class BillingDataAccessEvaluator {

    public boolean canAccessBillingData(String userId, UUID workspaceId) {
        return true;
    }
}
