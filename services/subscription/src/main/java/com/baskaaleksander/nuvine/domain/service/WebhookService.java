package com.baskaaleksander.nuvine.domain.service;

import com.stripe.model.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookService {

    public void handleEvent(Event event) {
        log.info("Received event: {}", event);
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
        log.info("Checkout session completed: {}", event.toJson());
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
        log.info("Customer subscription created: {}", event.toJson());
    }

}
