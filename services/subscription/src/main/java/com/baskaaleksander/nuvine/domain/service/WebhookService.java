package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.WorkspaceBillingTierUpdateRequest;
import com.baskaaleksander.nuvine.domain.model.*;
import com.baskaaleksander.nuvine.infrastructure.client.WorkspaceServiceClient;
import com.baskaaleksander.nuvine.infrastructure.persistence.PaymentSessionRepository;
import com.baskaaleksander.nuvine.infrastructure.persistence.PlanRepository;
import com.baskaaleksander.nuvine.infrastructure.persistence.SubscriptionRepository;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookService {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentSessionRepository paymentSessionRepository;
    private final WorkspaceServiceClient workspaceServiceClient;
    private final PlanRepository planRepository;

    public void handleEvent(Event event) {
        switch (event.getType()) {
            case "customer.subscription.created" -> handleCustomerSubscriptionCreated(event);
            case "customer.subscription.updated" -> handleCustomerSubscriptionUpdated(event);
            case "customer.subscription.deleted" -> handleCustomerSubscriptionDeleted(event);
            case "invoice.paid" -> handleInvoicePaid(event);
            case "invoice.payment_failed" -> handleInvoicePaymentFailed(event);
            case "invoice.payment_action_required" -> handleInvoicePaymentActionRequired(event);
            case "invoice.finalized" -> handleInvoiceFinalized(event);
            case "payment_intent.succeeded" -> handlePaymentIntentSucceeded(event);
            case "payment_intent.payment_failed" -> handlePaymentIntentPaymentFailed(event);
            case "charge.dispute.created" -> handleChargeDisputeCreated(event);
            case "charge.refunded" -> handleChargeRefunded(event);
            case "checkout.session.completed" -> handleCheckoutSessionCompleted(event);
            case "checkout.session.expired" -> handleCheckoutSessionExpired(event);
            default -> log.warn("Unknown event type: {}", event.getType());
        }
    }

    private void handleCheckoutSessionExpired(Event event) {

    }

    private void handleCheckoutSessionCompleted(Event event) {
        EventDataObjectDeserializer eventDataObjectDeserializer = event.getDataObjectDeserializer();
        Session checkoutSession;

        try {
            checkoutSession = (com.stripe.model.checkout.Session) eventDataObjectDeserializer.getObject().get();
        } catch (Exception e) {
            log.error("Failed to deserialize event data", e);
            return;
        }

        String id = checkoutSession.getId();
        PaymentSession paymentSession = paymentSessionRepository.findByStripeSessionId(id).orElse(null);

        if (paymentSession == null) {
            log.error("Payment session not found for session id: {}", id);
            return;
        }

        paymentSession.setCompletedAt(Instant.now());
        paymentSession.setStatus(PaymentSessionStatus.COMPLETED);
        paymentSessionRepository.save(paymentSession);
    }

    private void handleChargeRefunded(Event event) {
    }

    private void handleChargeDisputeCreated(Event event) {

    }

    private void handlePaymentIntentPaymentFailed(Event event) {

    }

    private void handlePaymentIntentSucceeded(Event event) {

    }

    private void handleInvoiceFinalized(Event event) {

    }

    private void handleInvoicePaymentActionRequired(Event event) {
    }

    private void handleInvoicePaymentFailed(Event event) {
    }

    private void handleInvoicePaid(Event event) {

    }

    private void handleCustomerSubscriptionDeleted(Event event) {

    }

    private void handleCustomerSubscriptionUpdated(Event event) {
        log.info("Customer subscription updated: {}", event.toJson());
    }

    private void handleCustomerSubscriptionCreated(Event event) {
        try {
            EventDataObjectDeserializer eventDataObjectDeserializer = event.getDataObjectDeserializer();
            com.stripe.model.Subscription stripeSubscription;

            try {
                stripeSubscription = (com.stripe.model.Subscription) eventDataObjectDeserializer.getObject().get();
            } catch (Exception e) {
                log.error("Failed to deserialize event data", e);
                return;
            }

            Map<String, String> metadata = stripeSubscription.getMetadata();
            String workspaceId = metadata.get("workspace_id");
            String planId = metadata.get("plan_id");

            String stripeCustomerId = stripeSubscription.getCustomer();
            String stripeSubscriptionId = stripeSubscription.getId();
            Boolean cancelAtPeriodEnd = stripeSubscription.getCancelAtPeriodEnd();

            SubscriptionItem item = stripeSubscription.getItems().getData().get(0);
            Long currentPeriodStart = item.getCurrentPeriodStart();
            Long currentPeriodEnd = item.getCurrentPeriodEnd();

            Instant now = Instant.now();


            Subscription subscription = Subscription.builder()
                    .workspaceId(UUID.fromString(workspaceId))
                    .planId(UUID.fromString(planId))
                    .stripeCustomerId(stripeCustomerId)
                    .stripeSubscriptionId(stripeSubscriptionId)
                    .status(SubscriptionStatus.ACTIVE)
                    .cancelAtPeriodEnd(cancelAtPeriodEnd)
                    .currentPeriodStart(Instant.ofEpochSecond(currentPeriodStart))
                    .currentPeriodEnd(Instant.ofEpochSecond(currentPeriodEnd))
                    .createdAt(now)
                    .updatedAt(now)
                    .build();


            subscriptionRepository.save(subscription);

            Plan plan = planRepository.findById(UUID.fromString(planId)).orElse(null);

            if (plan == null) {
                log.error("Plan not found for plan id: {}", planId);
                return;
            }

            workspaceServiceClient.updateWorkspaceBillingTier(UUID.fromString(workspaceId), new WorkspaceBillingTierUpdateRequest(plan.getCode()));
        } catch (Exception e) {
            log.error("Failed to handle customer subscription created event", e);
        }
    }

}
