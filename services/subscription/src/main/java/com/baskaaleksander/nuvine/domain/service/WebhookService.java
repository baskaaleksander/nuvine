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
        PaymentSession paymentSession = getPaymentSession(event);

        if (paymentSession == null) {
            log.error("Payment session not found for event id: {}", event.getId());
            return;
        }

        paymentSession.setCompletedAt(Instant.now());
        paymentSession.setStatus(PaymentSessionStatus.EXPIRED);
        paymentSessionRepository.save(paymentSession);
    }

    private void handleCheckoutSessionCompleted(Event event) {
        PaymentSession paymentSession = getPaymentSession(event);

        if (paymentSession == null) {
            log.error("Payment session not found for event id: {}", event.getId());
            return;
        }

        paymentSession.setCompletedAt(Instant.now());
        paymentSession.setStatus(PaymentSessionStatus.COMPLETED);
        paymentSessionRepository.save(paymentSession);
    }

    private PaymentSession getPaymentSession(Event event) {
        EventDataObjectDeserializer eventDataObjectDeserializer = event.getDataObjectDeserializer();
        Session checkoutSession;

        try {
            checkoutSession = (com.stripe.model.checkout.Session) eventDataObjectDeserializer.getObject().get();
        } catch (Exception e) {
            log.error("Failed to deserialize event data", e);
            return null;
        }

        String id = checkoutSession.getId();
        PaymentSession paymentSession = paymentSessionRepository.findByStripeSessionId(id).orElse(null);

        if (paymentSession == null) {
            log.error("Payment session not found for session id: {}", id);
            return null;
        }

        return paymentSession;
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
        try {
            EventDataObjectDeserializer eventDataObjectDeserializer = event.getDataObjectDeserializer();
            com.stripe.model.Invoice stripeInvoice;

            try {
                stripeInvoice = (com.stripe.model.Invoice) eventDataObjectDeserializer.getObject().get();
            } catch (Exception e) {
                log.error("Failed to deserialize event data", e);
                return;
            }

            String stripeSubscriptionId = null;
            if (stripeInvoice.getParent() != null &&
                    stripeInvoice.getParent().getSubscriptionDetails() != null) {
                stripeSubscriptionId = stripeInvoice.getParent().getSubscriptionDetails().getSubscription();
            }

            if (stripeSubscriptionId == null) {
                log.warn("Invoice has no associated subscription: {}", stripeInvoice.getId());
                return;
            }

            Subscription existingSubscription = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);

            if (existingSubscription == null) {
                log.error("Subscription not found for stripe subscription id: {}", stripeSubscriptionId);
                return;
            }

            existingSubscription.setStatus(SubscriptionStatus.PAST_DUE);
            existingSubscription.setUpdatedAt(Instant.now());

            subscriptionRepository.save(existingSubscription);

            workspaceServiceClient.updateWorkspaceBillingTier(
                    existingSubscription.getWorkspaceId(),
                    new WorkspaceBillingTierUpdateRequest("FREE")
            );

            log.warn("Invoice payment failed for subscription: {}", stripeSubscriptionId);
        } catch (Exception e) {
            log.error("Failed to handle invoice payment failed event", e);
        }
    }

    private void handleInvoicePaid(Event event) {
        try {
            EventDataObjectDeserializer eventDataObjectDeserializer = event.getDataObjectDeserializer();
            com.stripe.model.Invoice stripeInvoice;

            try {
                stripeInvoice = (com.stripe.model.Invoice) eventDataObjectDeserializer.getObject().get();
            } catch (Exception e) {
                log.error("Failed to deserialize event data", e);
                return;
            }

            String stripeSubscriptionId = null;
            if (stripeInvoice.getParent() != null &&
                    stripeInvoice.getParent().getSubscriptionDetails() != null) {
                stripeSubscriptionId = stripeInvoice.getParent().getSubscriptionDetails().getSubscription();
            }

            if (stripeSubscriptionId == null) {
                log.info("Invoice paid but no associated subscription: {}", stripeInvoice.getId());
                return;
            }

            Subscription existingSubscription = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);

            if (existingSubscription == null) {
                log.error("Subscription not found for stripe subscription id: {}", stripeSubscriptionId);
                return;
            }

            if (existingSubscription.getStatus() == SubscriptionStatus.PAST_DUE
                    || existingSubscription.getStatus() == SubscriptionStatus.UNPAID) {

                existingSubscription.setStatus(SubscriptionStatus.ACTIVE);
                existingSubscription.setUpdatedAt(Instant.now());


                Plan plan = planRepository.findById(existingSubscription.getPlanId()).orElse(null);

                if (plan == null) {
                    log.error("Plan not found for plan id: {}", existingSubscription.getPlanId());
                    return;
                }

                workspaceServiceClient.updateWorkspaceBillingTier(
                        existingSubscription.getWorkspaceId(),
                        new WorkspaceBillingTierUpdateRequest(plan.getCode())
                );

                log.info("Invoice paid, subscription restored to active: {}", stripeSubscriptionId);
            } else {
                log.info("Invoice paid for already active subscription: {}", stripeSubscriptionId);
            }
            existingSubscription.setCurrentPeriodStart(Instant.ofEpochSecond(stripeInvoice.getPeriodStart()));
            existingSubscription.setCurrentPeriodEnd(Instant.ofEpochSecond(stripeInvoice.getPeriodEnd()));
            subscriptionRepository.save(existingSubscription);

        } catch (Exception e) {
            log.error("Failed to handle invoice paid event", e);
        }
    }

    private void handleCustomerSubscriptionDeleted(Event event) {
        try {
            EventDataObjectDeserializer eventDataObjectDeserializer = event.getDataObjectDeserializer();
            com.stripe.model.Subscription stripeSubscription;

            try {
                stripeSubscription = (com.stripe.model.Subscription) eventDataObjectDeserializer.getObject().get();
            } catch (Exception e) {
                log.error("Failed to deserialize event data", e);
                return;
            }

            String stripeSubscriptionId = stripeSubscription.getId();

            Subscription existingSubscription = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);

            if (existingSubscription == null) {
                log.warn("Subscription not found for stripe subscription id: {}", stripeSubscriptionId);
                return;
            }

            existingSubscription.setStatus(SubscriptionStatus.DELETED);
            existingSubscription.setUpdatedAt(Instant.now());

            subscriptionRepository.save(existingSubscription);

            workspaceServiceClient.updateWorkspaceBillingTier(
                    existingSubscription.getWorkspaceId(),
                    new WorkspaceBillingTierUpdateRequest("FREE")
            );

            log.info("Customer subscription deleted successfully: {}", stripeSubscriptionId);
        } catch (Exception e) {
            log.error("Failed to handle customer subscription deleted event", e);
        }
    }

    private void handleCustomerSubscriptionUpdated(Event event) {
        try {
            EventDataObjectDeserializer eventDataObjectDeserializer = event.getDataObjectDeserializer();
            com.stripe.model.Subscription stripeSubscription;

            try {
                stripeSubscription = (com.stripe.model.Subscription) eventDataObjectDeserializer.getObject().get();
            } catch (Exception e) {
                log.error("Failed to deserialize event data", e);
                return;
            }

            String stripeSubscriptionId = stripeSubscription.getId();

            Subscription existingSubscription = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);

            if (existingSubscription == null) {
                log.error("Subscription not found for stripe subscription id: {}", stripeSubscriptionId);
                return;
            }

            Boolean cancelAtPeriodEnd = stripeSubscription.getCancelAtPeriodEnd();
            String stripeStatus = stripeSubscription.getStatus();

            SubscriptionItem item = stripeSubscription.getItems().getData().get(0);
            Long currentPeriodStart = item.getCurrentPeriodStart();
            Long currentPeriodEnd = item.getCurrentPeriodEnd();

            existingSubscription.setCancelAtPeriodEnd(cancelAtPeriodEnd);
            existingSubscription.setStatus(mapStripeStatus(stripeStatus));
            existingSubscription.setCurrentPeriodStart(Instant.ofEpochSecond(currentPeriodStart));
            existingSubscription.setCurrentPeriodEnd(Instant.ofEpochSecond(currentPeriodEnd));
            existingSubscription.setUpdatedAt(Instant.now());

            subscriptionRepository.save(existingSubscription);

            if ("active".equals(stripeStatus) || "canceled".equals(stripeStatus)) {
                Plan plan = planRepository.findById(existingSubscription.getPlanId()).orElse(null);

                if (plan == null) {
                    log.error("Plan not found for plan id: {}", existingSubscription.getPlanId());
                    return;
                }

                String billingTierCode = "canceled".equals(stripeStatus) ? "FREE" : plan.getCode();
                workspaceServiceClient.updateWorkspaceBillingTier(
                        existingSubscription.getWorkspaceId(),
                        new WorkspaceBillingTierUpdateRequest(billingTierCode)
                );
            }

            log.info("Customer subscription updated successfully: {}", stripeSubscriptionId);
        } catch (Exception e) {
            log.error("Failed to handle customer subscription updated event", e);
        }
    }

    private SubscriptionStatus mapStripeStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "active" -> SubscriptionStatus.ACTIVE;
            case "canceled" -> SubscriptionStatus.CANCELED;
            case "past_due" -> SubscriptionStatus.PAST_DUE;
            case "unpaid" -> SubscriptionStatus.UNPAID;
            default -> SubscriptionStatus.INCOMPLETE;
        };
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
