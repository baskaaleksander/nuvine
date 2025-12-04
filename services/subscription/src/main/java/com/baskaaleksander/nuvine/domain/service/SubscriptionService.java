package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.PaymentSessionResponse;
import com.baskaaleksander.nuvine.application.dto.UserInternalResponse;
import com.baskaaleksander.nuvine.application.dto.WorkspaceInternalSubscriptionResponse;
import com.baskaaleksander.nuvine.domain.exception.ForbiddenAccessException;
import com.baskaaleksander.nuvine.domain.exception.SubscriptionConflictException;
import com.baskaaleksander.nuvine.domain.exception.SubscriptionNotFoundException;
import com.baskaaleksander.nuvine.domain.model.*;
import com.baskaaleksander.nuvine.infrastructure.client.AuthServiceClient;
import com.baskaaleksander.nuvine.infrastructure.client.WorkspaceServiceClient;
import com.baskaaleksander.nuvine.infrastructure.persistence.PaymentSessionRepository;
import com.baskaaleksander.nuvine.infrastructure.persistence.PlanRepository;
import com.baskaaleksander.nuvine.infrastructure.persistence.SubscriptionRepository;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Invoice;
import com.stripe.model.StripeSearchResult;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerSearchParams;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
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
    private final PaymentSessionRepository paymentSessionRepository;

    @Value("${stripe.success-url}")
    private String successUrl;

    @Value("${stripe.cancel-url}")
    private String cancelUrl;

    public PaymentSessionResponse createPaymentSession(UUID workspaceId, UUID planId, PaymentSessionIntent intent, UUID userId) {

        var paymentSession = paymentSessionRepository.findValidSession(
                workspaceId,
                planId,
                PaymentSessionType.PAYMENT,
                intent,
                userId,
                Instant.now(),
                PaymentSessionStatus.PENDING
        ).orElse(null);

        if (paymentSession != null) {
            return new PaymentSessionResponse(paymentSession.getStripeUrl(), paymentSession.getStripeSessionId());
        }

        var plan = planRepository.findById(planId).orElseThrow(() -> new RuntimeException("Plan not found"));

        var subscription = subscriptionRepository.findByWorkspaceId(workspaceId).orElse(null);

        if (intent == PaymentSessionIntent.SUBSCRIPTION_CREATE && subscription != null) {
            throw new SubscriptionConflictException("Subscription already exists");
        } else if (intent == PaymentSessionIntent.SUBSCRIPTION_UPDATE && subscription == null) {
            throw new SubscriptionNotFoundException("Subscription not found");
        }

        WorkspaceInternalSubscriptionResponse workspaceSubscriptionResponse = searchWorkspace(workspaceId);
        UserInternalResponse userInternalResponse = searchUser(userId);

        if (!workspaceSubscriptionResponse.ownerId().equals(userInternalResponse.id())) {
            throw new ForbiddenAccessException("User is not the owner of the workspace");
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
        Map<String, String> metadata = new HashMap<>();
        metadata.put("workspace_id", workspaceId.toString());
        metadata.put("plan_id", planId.toString());
        metadata.put("user_id", userId.toString());

        switch (intent) {
            case SUBSCRIPTION_CREATE ->
                    response = createNewSubscriptionSession(plan.getStripePriceId(), searchResult, userInternalResponse.email(), metadata);
            case SUBSCRIPTION_UPDATE ->
                    response = createUpdateSubscriptionSession(plan.getStripePriceId(), subscription);
            default -> throw new IllegalStateException("Unexpected value: " + intent);
        }

        PaymentSession session = PaymentSession.builder()
                .workspaceId(workspaceId)
                .planId(planId)
                .userId(userId)
                .type(PaymentSessionType.PAYMENT)
                .intent(intent)
                .stripeSessionId(response.sessionId())
                .stripeUrl(response.url())
                .status(PaymentSessionStatus.PENDING)
                .expiresAt(Instant.now().plusSeconds(24 * 60 * 60))
                .metadataJson(metadata)
                .build();

        paymentSessionRepository.save(session);

        return response;
    }

    private PaymentSessionResponse createNewSubscriptionSession(String planPriceId, StripeSearchResult<Customer> searchResult, String email, Map<String, String> metadata) {

        String customerId;

        if (searchResult.getData().isEmpty()) {
            CustomerCreateParams customerCreateParams = CustomerCreateParams.builder()
                    .setEmail(email)
                    .build();

            try {
                Customer customer = stripeClient.v1().customers().create(customerCreateParams);
                customerId = customer.getId();
            } catch (StripeException e) {
                log.error("Failed to create customer", e);
                throw new RuntimeException("Failed to create Stripe customer, try again later.");
            }
        } else {
            Customer customer = searchResult.getData().get(0);
            customerId = customer.getId();
        }

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPrice(planPriceId)
                                .setQuantity(1L)
                                .build()
                )
                .setCustomer(customerId)
                .putAllMetadata(metadata)
                .build();

        Session session;

        try {
            session = stripeClient.v1().checkout().sessions().create(params);

        } catch (StripeException e) {
            log.error("Failed to create payment session", e);
            throw new RuntimeException("Failed to create Stripe payment session, try again later.");
        }
        return new PaymentSessionResponse(session.getUrl(), session.getId());
    }

    private PaymentSessionResponse createUpdateSubscriptionSession(String newPlanPriceId, Subscription subscription) {
        try {
            com.stripe.model.Subscription stripeSubscription = stripeClient.v1().subscriptions()
                    .retrieve(subscription.getStripeSubscriptionId());

            String subscriptionItemId = stripeSubscription.getItems().getData().get(0).getId();

            SubscriptionUpdateParams updateParams = SubscriptionUpdateParams.builder()
                    .addItem(SubscriptionUpdateParams.Item.builder()
                            .setId(subscriptionItemId)
                            .setPrice(newPlanPriceId)
                            .build())
                    .setProrationBehavior(SubscriptionUpdateParams.ProrationBehavior.ALWAYS_INVOICE)
                    .build();

            com.stripe.model.Subscription updatedSubscription = stripeClient.v1().subscriptions()
                    .update(subscription.getStripeSubscriptionId(), updateParams);

            String invoiceId = updatedSubscription.getLatestInvoice();
            Invoice invoice = stripeClient.v1().invoices().retrieve(invoiceId);

            if ("paid".equals(invoice.getStatus())) {
                return new PaymentSessionResponse(successUrl, "sub_update_" + updatedSubscription.getId());
            } else {
                return new PaymentSessionResponse(invoice.getHostedInvoiceUrl(), invoice.getId());
            }

        } catch (StripeException e) {
            log.error("Failed to update subscription", e);
            throw new RuntimeException("Failed to update Stripe subscription, try again later.");
        }
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
