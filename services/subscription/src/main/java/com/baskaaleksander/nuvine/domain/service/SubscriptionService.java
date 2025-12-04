package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.PaymentSessionResponse;
import com.baskaaleksander.nuvine.application.dto.UserInternalResponse;
import com.baskaaleksander.nuvine.application.dto.WorkspaceInternalSubscriptionResponse;
import com.baskaaleksander.nuvine.domain.model.PaymentSessionIntent;
import com.baskaaleksander.nuvine.infrastructure.client.AuthServiceClient;
import com.baskaaleksander.nuvine.infrastructure.client.WorkspaceServiceClient;
import com.baskaaleksander.nuvine.infrastructure.persistence.PlanRepository;
import com.baskaaleksander.nuvine.infrastructure.persistence.SubscriptionRepository;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.StripeSearchResult;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerSearchParams;
import com.stripe.param.checkout.SessionCreateParams;
import feign.FeignException;
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
    private final SubscriptionRepository subscriptionRepository;
    private final AuthServiceClient authServiceClient;
    private final WorkspaceServiceClient workspaceServiceClient;

    public PaymentSessionResponse createPaymentSession(UUID workspaceId, UUID planId, PaymentSessionIntent intent, UUID userId) {

        var plan = planRepository.findById(planId).orElseThrow(() -> new RuntimeException("Plan not found"));

        var subscription = subscriptionRepository.findByWorkspaceId(workspaceId).orElse(null);

        if (intent == PaymentSessionIntent.SUBSCRIPTION_CREATE && subscription != null) {
            throw new RuntimeException("Subscription already exists");
        } else if (intent == PaymentSessionIntent.SUBSCRIPTION_UPDATE && subscription == null) {
            throw new RuntimeException("Subscription not found");
        }

        WorkspaceInternalSubscriptionResponse workspaceSubscriptionResponse = searchWorkspace(workspaceId);
        UserInternalResponse userInternalResponse = searchUser(userId);

        if (!workspaceSubscriptionResponse.ownerId().equals(userInternalResponse.id())) {
            throw new RuntimeException("User is not the owner of the workspace");
        }

        CustomerSearchParams params = CustomerSearchParams.builder()
                .setQuery("email:'" + userInternalResponse.email() + "'")
                .build();

        StripeSearchResult<Customer> searchResult;

        try {
            searchResult = stripeClient.v1().customers().search(params);
        } catch (StripeException e) {
            log.error("Failed to search customer", e);
            throw new RuntimeException(e.getMessage());
        }

        PaymentSessionResponse response;

        switch (intent) {
            case SUBSCRIPTION_CREATE ->
                    response = createNewSubscriptionSession(plan.getStripePriceId(), searchResult, userInternalResponse.email());
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

    private WorkspaceInternalSubscriptionResponse searchWorkspace(UUID workspaceId) {
        try {
            return workspaceServiceClient.getWorkspaceSubscription(workspaceId);
        } catch (FeignException e) {
            int status = e.status();
            if (status == 404) {
                throw new RuntimeException("Workspace not found");
            } else {
                log.error("SEARCH_WORKSPACE FAILED", e);
                throw e;
            }
        } catch (Exception e) {
            log.error("SEARCH_WORKSPACE FAILED", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    private UserInternalResponse searchUser(UUID userId) {
        try {
            return authServiceClient.getUserInternalResponse(userId);
        } catch (FeignException e) {
            int status = e.status();
            if (status == 404) {
                log.info("SEARCH_USER FAILED reason=user_not_found");
                throw new RuntimeException("User not found");
            } else {
                log.error("SEARCH_USER FAILED", e);
                throw e;
            }
        } catch (Exception e) {
            log.error("SEARCH_USER FAILED", e);
            throw e;
        }
    }
}
