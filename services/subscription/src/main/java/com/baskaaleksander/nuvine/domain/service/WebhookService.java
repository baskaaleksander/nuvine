package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.UserInternalResponse;
import com.baskaaleksander.nuvine.application.dto.WorkspaceBillingTierUpdateRequest;
import com.baskaaleksander.nuvine.application.dto.WorkspaceInternalSubscriptionResponse;
import com.baskaaleksander.nuvine.domain.exception.UserNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.WorkspaceNotFoundException;
import com.baskaaleksander.nuvine.domain.model.*;
import com.baskaaleksander.nuvine.infrastructure.client.AuthServiceCacheWrapper;
import com.baskaaleksander.nuvine.infrastructure.client.WorkspaceServiceCacheWrapper;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.PaymentActionRequiredEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.PaymentActionRequiredEventProducer;
import com.baskaaleksander.nuvine.infrastructure.persistence.PaymentRepository;
import com.baskaaleksander.nuvine.infrastructure.persistence.PaymentSessionRepository;
import com.baskaaleksander.nuvine.infrastructure.persistence.SubscriptionRepository;
import com.stripe.StripeClient;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.checkout.Session;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionCacheService subscriptionCacheService;
    private final PaymentSessionRepository paymentSessionRepository;
    private final WorkspaceServiceCacheWrapper workspaceServiceCacheWrapper;
    private final PaymentRepository paymentRepository;
    private final PlanService planService;
    private final PaymentActionRequiredEventProducer paymentActionRequiredEventProducer;
    private final AuthServiceCacheWrapper authServiceCacheWrapper;
    private final StripeClient stripeClient;


    //todo: implement payment's logging related webhooks
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

    private void handlePaymentIntentPaymentFailed(Event event) {
        try {
            EventDataObjectDeserializer eventDataObjectDeserializer = event.getDataObjectDeserializer();
            com.stripe.model.PaymentIntent paymentIntent;

            try {
                paymentIntent = (com.stripe.model.PaymentIntent) eventDataObjectDeserializer.getObject().get();
            } catch (Exception e) {
                log.error("Failed to deserialize event data", e);
                return;
            }

            Payment payment = paymentRepository.findByStripePaymentIntentId(paymentIntent.getId())
                    .orElse(null);

            if (payment != null) {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                log.warn("Payment intent failed, updated payment: {}", payment.getId());
            } else {
                log.warn("Payment intent failed but no matching payment found: {}", paymentIntent.getId());
            }

        } catch (Exception e) {
            log.error("Failed to handle payment intent failed event", e);
        }
    }

    private void handlePaymentIntentSucceeded(Event event) {
        try {
            EventDataObjectDeserializer eventDataObjectDeserializer = event.getDataObjectDeserializer();
            com.stripe.model.PaymentIntent paymentIntent;

            try {
                paymentIntent = (com.stripe.model.PaymentIntent) eventDataObjectDeserializer.getObject().get();
            } catch (Exception e) {
                log.error("Failed to deserialize event data", e);
                return;
            }

            Payment payment = paymentRepository.findByStripePaymentIntentId(paymentIntent.getId())
                    .orElse(null);

            if (payment != null) {
                payment.setStatus(PaymentStatus.SUCCEEDED);
                payment.setAmountPaid(convertStripeAmount(paymentIntent.getAmountReceived()));
                paymentRepository.save(payment);
                log.info("Payment intent succeeded, updated payment: {}", payment.getId());
            } else {
                log.info("Payment intent succeeded but no matching payment found: {}", paymentIntent.getId());
            }

        } catch (Exception e) {
            log.error("Failed to handle payment intent succeeded event", e);
        }
    }

    private void handleInvoiceFinalized(Event event) {
        try {
            EventDataObjectDeserializer eventDataObjectDeserializer = event.getDataObjectDeserializer();
            com.stripe.model.Invoice stripeInvoice;

            try {
                stripeInvoice = (com.stripe.model.Invoice) eventDataObjectDeserializer.getObject().get();
            } catch (Exception e) {
                log.error("Failed to deserialize event data", e);
                return;
            }

            String stripeSubscriptionId = extractSubscriptionId(stripeInvoice);

            if (stripeSubscriptionId == null) {
                log.info("Invoice finalized but no associated subscription: {}", stripeInvoice.getId());
                return;
            }

            Subscription existingSubscription = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);

            if (existingSubscription == null) {
                log.error("Subscription not found for stripe subscription id: {}", stripeSubscriptionId);
                return;
            }

            Payment payment = createOrUpdatePaymentFromInvoice(stripeInvoice, existingSubscription);
            payment.setStatus(PaymentStatus.PENDING);
            paymentRepository.save(payment);
            log.info("Payment pending recorded for finalized invoice: {}", stripeInvoice.getId());

        } catch (Exception e) {
            log.error("Failed to handle invoice finalized event", e);
        }
    }

    private void handleInvoicePaymentActionRequired(Event event) {
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
                log.info("Invoice payment action required but no associated subscription: {}", stripeInvoice.getId());
                return;
            }

            Subscription existingSubscription = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);

            if (existingSubscription == null) {
                log.error("Subscription not found for stripe subscription id: {}", stripeSubscriptionId);
                return;
            }

            Payment payment = createOrUpdatePaymentFromInvoice(stripeInvoice, existingSubscription);
            payment.setStatus(PaymentStatus.REQUIRES_ACTION);
            paymentRepository.save(payment);
            log.info("Payment action required recorded for invoice: {}", stripeInvoice.getId());

            WorkspaceInternalSubscriptionResponse workspace = searchWorkspace(existingSubscription.getWorkspaceId());
            UserInternalResponse owner = searchUser(workspace.ownerId());


            existingSubscription.setStatus(SubscriptionStatus.INCOMPLETE);
            existingSubscription.setUpdatedAt(Instant.now());

            subscriptionRepository.save(existingSubscription);
            subscriptionCacheService.evictSubscriptionCache(existingSubscription.getWorkspaceId());

            String hostedInvoiceUrl = stripeInvoice.getHostedInvoiceUrl();

            paymentActionRequiredEventProducer.producePaymentActionRequiredEvent(
                    new PaymentActionRequiredEvent(
                            owner.email(),
                            stripeInvoice.getId(),
                            hostedInvoiceUrl,
                            workspace.id().toString(),
                            workspace.name(),
                            owner.id().toString()
                    )
            );


            log.warn("Invoice payment action required for subscription: {}. User notified.", stripeSubscriptionId);

        } catch (Exception e) {
            log.error("Failed to handle invoice payment action required event", e);
        }
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

            Payment payment = createOrUpdatePaymentFromInvoice(stripeInvoice, existingSubscription);
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.warn("Payment failure recorded for invoice: {}", stripeInvoice.getId());

            existingSubscription.setStatus(SubscriptionStatus.PAST_DUE);
            existingSubscription.setUpdatedAt(Instant.now());

            subscriptionRepository.save(existingSubscription);
            subscriptionCacheService.evictSubscriptionCache(existingSubscription.getWorkspaceId());

            workspaceServiceCacheWrapper.updateWorkspaceBillingTier(
                    existingSubscription.getWorkspaceId(),
                    new WorkspaceBillingTierUpdateRequest("FREE")
            );

            log.warn("Invoice payment failed for subscription: {}", stripeSubscriptionId);
        } catch (Exception e) {
            log.error("Failed to handle invoice payment failed event", e);
        }
    }

    // todo add payment monitoring
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

            Payment payment = createOrUpdatePaymentFromInvoice(stripeInvoice, existingSubscription);
            payment.setStatus(PaymentStatus.SUCCEEDED);
            paymentRepository.save(payment);
            log.info("Payment saved for subscription id: {}", stripeSubscriptionId);

            if (existingSubscription.getStatus() == SubscriptionStatus.PAST_DUE
                    || existingSubscription.getStatus() == SubscriptionStatus.UNPAID) {

                existingSubscription.setStatus(SubscriptionStatus.ACTIVE);
                existingSubscription.setUpdatedAt(Instant.now());


                Plan plan = planService.findById(existingSubscription.getPlanId()).orElse(null);

                if (plan == null) {
                    log.error("Plan not found for plan id: {}", existingSubscription.getPlanId());
                    return;
                }

                workspaceServiceCacheWrapper.updateWorkspaceBillingTier(
                        existingSubscription.getWorkspaceId(),
                        new WorkspaceBillingTierUpdateRequest(plan.getCode())
                );

                log.info("Invoice paid, subscription restored to active: {}", stripeSubscriptionId);
            } else {
                log.info("Invoice paid for already active subscription: {}", stripeSubscriptionId);
            }
            String subscriptionId =
                    stripeInvoice.getParent()
                            .getSubscriptionDetails()
                            .getSubscription();

            com.stripe.model.Subscription stripeSubscription =
                    stripeClient.v1().subscriptions().retrieve(subscriptionId);

            Long periodStart = stripeSubscription.getItems().getData().get(0).getCurrentPeriodStart();
            Long periodEnd = stripeSubscription.getItems().getData().get(0).getCurrentPeriodEnd();
            existingSubscription.setCurrentPeriodStart(Instant.ofEpochSecond(periodStart));
            existingSubscription.setCurrentPeriodEnd(Instant.ofEpochSecond(periodEnd));
            subscriptionRepository.save(existingSubscription);
            subscriptionCacheService.evictSubscriptionCache(existingSubscription.getWorkspaceId());

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
            subscriptionCacheService.evictSubscriptionCache(existingSubscription.getWorkspaceId());

            workspaceServiceCacheWrapper.updateWorkspaceBillingTier(
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
            Map<String, String> metadata = stripeSubscription.getMetadata();
            String planId = metadata.get("plan_id");
            String workspaceId = metadata.get("workspace_id");

            SubscriptionItem item = stripeSubscription.getItems().getData().get(0);
            Long currentPeriodStart = item.getCurrentPeriodStart();
            Long currentPeriodEnd = item.getCurrentPeriodEnd();


            existingSubscription.setCancelAtPeriodEnd(cancelAtPeriodEnd);
            existingSubscription.setStatus(mapStripeStatus(stripeStatus));
            existingSubscription.setCurrentPeriodStart(Instant.ofEpochSecond(currentPeriodStart));
            existingSubscription.setCurrentPeriodEnd(Instant.ofEpochSecond(currentPeriodEnd));
            existingSubscription.setPlanId(UUID.fromString(planId));
            existingSubscription.setUpdatedAt(Instant.now());

            subscriptionRepository.save(existingSubscription);
            subscriptionCacheService.evictSubscriptionCache(existingSubscription.getWorkspaceId());

            if ("active".equals(stripeStatus) || "canceled".equals(stripeStatus)) {
                Plan plan = planService.findById(UUID.fromString(planId)).orElse(null);

                if (plan == null) {
                    log.error("Plan not found for plan id: {}", existingSubscription.getPlanId());
                    return;
                }

                log.info("Updating workspace billing tier for workspace id: {}", existingSubscription.getWorkspaceId());

                String billingTierCode = "canceled".equals(stripeStatus) ? "FREE" : plan.getCode();
                workspaceServiceCacheWrapper.updateWorkspaceBillingTier(
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

            Subscription subscription = Subscription.builder()
                    .workspaceId(UUID.fromString(workspaceId))
                    .planId(UUID.fromString(planId))
                    .stripeCustomerId(stripeCustomerId)
                    .stripeSubscriptionId(stripeSubscriptionId)
                    .status(SubscriptionStatus.ACTIVE)
                    .cancelAtPeriodEnd(cancelAtPeriodEnd)
                    .currentPeriodStart(Instant.ofEpochSecond(currentPeriodStart))
                    .currentPeriodEnd(Instant.ofEpochSecond(currentPeriodEnd))
                    .build();

            subscriptionRepository.save(subscription);
            subscriptionCacheService.evictSubscriptionCache(UUID.fromString(workspaceId));

            Plan plan = planService.findById(UUID.fromString(planId)).orElse(null);

            if (plan == null) {
                log.error("Plan not found for plan id: {}", planId);
                return;
            }

            workspaceServiceCacheWrapper.updateWorkspaceBillingTier(UUID.fromString(workspaceId), new WorkspaceBillingTierUpdateRequest(plan.getCode()));
        } catch (Exception e) {
            log.error("Failed to handle customer subscription created event", e);
        }
    }

    private WorkspaceInternalSubscriptionResponse searchWorkspace(UUID workspaceId) {
        try {
            return workspaceServiceCacheWrapper.getWorkspaceSubscription(workspaceId);
        } catch (FeignException e) {
            int status = e.status();
            if (status == 404) {
                throw new WorkspaceNotFoundException("Workspace not found");
            } else {
                log.error("SEARCH_WORKSPACE FAILED", e);
                throw new RuntimeException("Failed to search user, try again later.");
            }
        } catch (Exception e) {
            log.error("SEARCH_WORKSPACE FAILED", e);
            throw new RuntimeException("Failed to search workspace, try again later.");
        }
    }

    private UserInternalResponse searchUser(UUID userId) {
        try {
            return authServiceCacheWrapper.getUserInternalResponse(userId);
        } catch (FeignException e) {
            int status = e.status();
            if (status == 404) {
                log.info("SEARCH_USER FAILED reason=user_not_found");
                throw new UserNotFoundException("User not found");
            } else {
                log.error("SEARCH_USER FAILED", e);
                throw new RuntimeException("Failed to search user, try again later.");
            }
        } catch (Exception e) {
            log.error("SEARCH_USER FAILED", e);
            throw new RuntimeException("Failed to search user, try again later.");
        }
    }

    private Payment createOrUpdatePaymentFromInvoice(com.stripe.model.Invoice stripeInvoice,
                                                     Subscription subscription) {
        Payment payment = paymentRepository.findByStripeInvoiceId(stripeInvoice.getId())
                .orElse(new Payment());

        payment.setWorkspaceId(subscription.getWorkspaceId());
        payment.setSubscriptionId(subscription.getId());
        payment.setStripeInvoiceId(stripeInvoice.getId());

        String stripePaymentIntent = extractPaymentIntentId(stripeInvoice);
        payment.setStripePaymentIntentId(stripePaymentIntent);

        payment.setAmountDue(convertStripeAmount(stripeInvoice.getAmountDue()));
        payment.setAmountPaid(convertStripeAmount(stripeInvoice.getAmountPaid()));
        payment.setCurrency(stripeInvoice.getCurrency().toUpperCase());
        payment.setBillingPeriodStart(Instant.ofEpochSecond(stripeInvoice.getPeriodStart()));
        payment.setBillingPeriodEnd(Instant.ofEpochSecond(stripeInvoice.getPeriodEnd()));
        payment.setInvoicePdfUrl(stripeInvoice.getInvoicePdf());
        payment.setDescription(buildPaymentDescription(stripeInvoice));

        if (stripeInvoice.getMetadata() != null && !stripeInvoice.getMetadata().isEmpty()) {
            payment.setMetadataJson(convertMetadataToJson(stripeInvoice.getMetadata()));
        }

        return payment;
    }

    private BigDecimal convertStripeAmount(Long amountInCents) {
        if (amountInCents == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(amountInCents).divide(BigDecimal.valueOf(100));
    }

    private String buildPaymentDescription(com.stripe.model.Invoice invoice) {
        if (invoice.getLines() != null && !invoice.getLines().getData().isEmpty()) {
            return invoice.getLines().getData().get(0).getDescription();
        }
        return "Subscription payment";
    }

    private String convertMetadataToJson(Map<String, String> metadata) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(metadata);
        } catch (Exception e) {
            log.error("Failed to convert metadata to JSON", e);
            return "{}";
        }
    }

    private String extractPaymentIntentId(com.stripe.model.Invoice invoice) {
        if (invoice.getPayments() != null && invoice.getPayments().getData() != null
                && !invoice.getPayments().getData().isEmpty()) {
            return invoice.getPayments().getData().get(0).getPayment().getPaymentIntent();
        }
        return null;
    }

    private String extractSubscriptionId(com.stripe.model.Invoice invoice) {
        if (invoice.getParent() != null &&
                invoice.getParent().getSubscriptionDetails() != null) {
            return invoice.getParent().getSubscriptionDetails().getSubscription();
        }
        return null;
    }
}
