package com.baskaaleksander.subscription.domain.service;

import com.baskaaleksander.subscription.application.dto.PaymentSessionResponse;
import com.baskaaleksander.subscription.domain.model.PaymentSessionIntent;
import com.baskaaleksander.subscription.infrastructure.persistence.PlanRepository;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.StripeSearchResult;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerSearchParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final StripeClient stripeClient;
    private final PlanRepository planRepository;

    public PaymentSessionResponse createPaymentSession(UUID workspaceId, UUID planId, PaymentSessionIntent intent, UUID userId, String email) {
        var plan = planRepository.findById(planId).orElseThrow(() -> new RuntimeException("Plan not found"));
        CustomerSearchParams params = CustomerSearchParams.builder()
                .setQuery("email:'" + email + "'")
                .build();

        StripeSearchResult searchResult;

        try {
            searchResult = stripeClient.v1().customers().search(params);
        } catch (StripeException e) {
            log.error("Failed to search customer", e);
            throw new RuntimeException(e.getMessage());
        }

        PaymentSessionResponse response;

        switch (intent) {
            case SUBSCRIPTION_CREATE ->
                    response = createNewSubscriptionSession(plan.getStripePriceId(), searchResult, email);
            case SUBSCRIPTION_UPDATE ->
                    response = createUpdateSubscriptionSession(plan.getStripePriceId(), searchResult);
            default -> throw new IllegalStateException("Unexpected value: " + intent);
        }

        return response;
    }

    private PaymentSessionResponse createNewSubscriptionSession(String planPriceId, StripeSearchResult searchResult, String email) {
        SessionCreateParams params;

        if (searchResult.getData().isEmpty()) {
            CustomerCreateParams customerCreateParams = CustomerCreateParams.builder()
                    .setEmail(email)
                    .build();

            Customer customer;

            try {
                customer = stripeClient.v1().customers().create(customerCreateParams);
            } catch (StripeException e) {
                log.error("Failed to create customer", e);
                throw new RuntimeException(e.getMessage());
            }
            params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setSuccessUrl("http://localhost:3000")
                    .setCancelUrl("http://localhost:3000")
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPrice(planPriceId)
                                    .setQuantity(1L)
                                    .build()
                    )
                    .setCustomer(customer.getId())
                    .build();
        } else {
            var customer = (Customer) searchResult.getData().getFirst();

            params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setSuccessUrl("http://localhost:3000")
                    .setCancelUrl("http://localhost:3000")
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPrice(planPriceId)
                                    .setQuantity(1L)
                                    .build()
                    )
                    .setCustomer(customer.getId())
                    .build();
        }


        Session session;

        try {
            session = stripeClient.v1().checkout().sessions().create(params);

        } catch (StripeException e) {
            log.error("Failed to create payment session", e);
            throw new RuntimeException(e.getMessage());
        }
        return new PaymentSessionResponse(session.getUrl());
    }

    private PaymentSessionResponse createUpdateSubscriptionSession(String planPriceId, StripeSearchResult searchResult) {
        return null;
    }
}
