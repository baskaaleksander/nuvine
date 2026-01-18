package com.baskaaleksander.nuvine.integration.controller;

import com.baskaaleksander.nuvine.domain.model.*;
import com.baskaaleksander.nuvine.infrastructure.persistence.*;
import com.baskaaleksander.nuvine.integration.base.BaseControllerIntegrationTest;
import com.baskaaleksander.nuvine.integration.support.StripeTestUtils;
import com.baskaaleksander.nuvine.integration.support.TestDataBuilder;
import com.baskaaleksander.nuvine.integration.support.WireMockStubs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookControllerIT extends BaseControllerIntegrationTest {

    @Autowired
    private TestDataBuilder testDataBuilder;

    @Autowired
    private PaymentSessionRepository paymentSessionRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private WireMockStubs wireMockStubs;
    private UUID workspaceId;
    private UUID userId;
    private UUID planId;

    @BeforeEach
    void setUp() {
        testDataBuilder.cleanUp();
        wireMockStubs = new WireMockStubs(wireMockServer);
        workspaceId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    private HttpHeaders webhookHeaders(String payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Stripe-Signature", StripeTestUtils.generateWebhookSignature(
                payload, StripeTestUtils.TEST_WEBHOOK_SECRET));
        return headers;
    }

    @Nested
    @DisplayName("Checkout Session Events")
    class CheckoutSessionEvents {

        @Test
        @DisplayName("Should update payment session status on checkout.session.completed")
        void shouldUpdatePaymentSessionOnCompleted() {
            Plan plan = testDataBuilder.createProPlan();
            planId = plan.getId();
            PaymentSession session = testDataBuilder.createPaymentSession(
                    workspaceId, userId, plan, PaymentSessionStatus.PENDING);

            String payload = StripeTestUtils.checkoutSessionCompletedEvent(
                    session.getStripeSessionId(), workspaceId, planId);

            ResponseEntity<Void> response = restTemplate.exchange(
                    "/api/v1/stripe/webhook",
                    HttpMethod.POST,
                    new HttpEntity<>(payload, webhookHeaders(payload)),
                    Void.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            Optional<PaymentSession> updatedSession = paymentSessionRepository
                    .findByStripeSessionId(session.getStripeSessionId());
            assertThat(updatedSession).isPresent();
            assertThat(updatedSession.get().getStatus()).isEqualTo(PaymentSessionStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should mark session expired on checkout.session.expired")
        void shouldMarkSessionExpiredOnExpired() {
            Plan plan = testDataBuilder.createProPlan();
            PaymentSession session = testDataBuilder.createPaymentSession(
                    workspaceId, userId, plan, PaymentSessionStatus.PENDING);

            String payload = StripeTestUtils.checkoutSessionExpiredEvent(session.getStripeSessionId());

            ResponseEntity<Void> response = restTemplate.exchange(
                    "/api/v1/stripe/webhook",
                    HttpMethod.POST,
                    new HttpEntity<>(payload, webhookHeaders(payload)),
                    Void.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            Optional<PaymentSession> updatedSession = paymentSessionRepository
                    .findByStripeSessionId(session.getStripeSessionId());
            assertThat(updatedSession).isPresent();
            assertThat(updatedSession.get().getStatus()).isEqualTo(PaymentSessionStatus.EXPIRED);
        }
    }

    @Nested
    @DisplayName("Subscription Events")
    class SubscriptionEvents {

        @Test
        @DisplayName("Should create subscription on customer.subscription.created")
        void shouldCreateSubscriptionOnCreated() {
            Plan plan = testDataBuilder.createProPlan();
            planId = plan.getId();

            wireMockStubs.stubWorkspaceServiceUpdateBillingTier(workspaceId);

            String stripeSubscriptionId = "sub_" + UUID.randomUUID().toString().substring(0, 14);
            String stripeCustomerId = "cus_" + UUID.randomUUID().toString().substring(0, 14);
            long periodStart = Instant.now().getEpochSecond();
            long periodEnd = Instant.now().plusSeconds(2592000).getEpochSecond();

            String payload = StripeTestUtils.subscriptionCreatedEvent(
                    stripeSubscriptionId, stripeCustomerId, workspaceId, planId, periodStart, periodEnd);

            ResponseEntity<Void> response = restTemplate.exchange(
                    "/api/v1/stripe/webhook",
                    HttpMethod.POST,
                    new HttpEntity<>(payload, webhookHeaders(payload)),
                    Void.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            Subscription subscription = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);
            assertThat(subscription).isNotNull();
            assertThat(subscription.getWorkspaceId()).isEqualTo(workspaceId);
            assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should update subscription on customer.subscription.updated")
        void shouldUpdateSubscriptionOnUpdated() {
            Plan plan = testDataBuilder.createProPlan();
            Subscription subscription = testDataBuilder.createActiveSubscription(workspaceId, plan);

            wireMockStubs.stubWorkspaceServiceUpdateBillingTier(workspaceId);

            long periodStart = Instant.now().getEpochSecond();
            long periodEnd = Instant.now().plusSeconds(2592000).getEpochSecond();

            String payload = StripeTestUtils.subscriptionUpdatedEvent(
                    subscription.getStripeSubscriptionId(), "active",
                    workspaceId, plan.getId(), periodStart, periodEnd, true);

            ResponseEntity<Void> response = restTemplate.exchange(
                    "/api/v1/stripe/webhook",
                    HttpMethod.POST,
                    new HttpEntity<>(payload, webhookHeaders(payload)),
                    Void.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            Subscription updated = subscriptionRepository.findByStripeSubscriptionId(
                    subscription.getStripeSubscriptionId());
            assertThat(updated).isNotNull();
            assertThat(updated.getCancelAtPeriodEnd()).isTrue();
        }

        @Test
        @DisplayName("Should mark subscription deleted on customer.subscription.deleted")
        void shouldMarkSubscriptionDeletedOnDeleted() {
            Plan plan = testDataBuilder.createProPlan();
            Subscription subscription = testDataBuilder.createActiveSubscription(workspaceId, plan);

            wireMockStubs.stubWorkspaceServiceUpdateBillingTier(workspaceId);

            long periodStart = Instant.now().getEpochSecond();
            long periodEnd = Instant.now().plusSeconds(2592000).getEpochSecond();

            String payload = StripeTestUtils.subscriptionDeletedEvent(
                    subscription.getStripeSubscriptionId(), workspaceId, plan.getId(), periodStart, periodEnd);

            ResponseEntity<Void> response = restTemplate.exchange(
                    "/api/v1/stripe/webhook",
                    HttpMethod.POST,
                    new HttpEntity<>(payload, webhookHeaders(payload)),
                    Void.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            Subscription deleted = subscriptionRepository.findByStripeSubscriptionId(
                    subscription.getStripeSubscriptionId());
            assertThat(deleted).isNotNull();
            assertThat(deleted.getStatus()).isEqualTo(SubscriptionStatus.DELETED);
        }
    }

    @Nested
    @DisplayName("Invoice Events")
    class InvoiceEvents {

        @Test
        @DisplayName("Should create payment record on invoice.finalized")
        void shouldCreatePaymentOnFinalized() {
            Plan plan = testDataBuilder.createProPlan();
            Subscription subscription = testDataBuilder.createActiveSubscription(workspaceId, plan);

            String invoiceId = "in_" + UUID.randomUUID().toString().substring(0, 14);
            String payload = StripeTestUtils.invoiceFinalizedEvent(
                    invoiceId, subscription.getStripeSubscriptionId(), 2999L, "usd");

            ResponseEntity<Void> response = restTemplate.exchange(
                    "/api/v1/stripe/webhook",
                    HttpMethod.POST,
                    new HttpEntity<>(payload, webhookHeaders(payload)),
                    Void.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            Optional<Payment> payment = paymentRepository.findByStripeInvoiceId(invoiceId);
            assertThat(payment).isPresent();
            assertThat(payment.get().getStatus()).isEqualTo(PaymentStatus.PENDING);
        }

        @Test
        @DisplayName("Should update payment status on invoice.payment_failed")
        void shouldUpdatePaymentStatusOnFailed() {
            Plan plan = testDataBuilder.createProPlan();
            Subscription subscription = testDataBuilder.createActiveSubscription(workspaceId, plan);

            wireMockStubs.stubWorkspaceServiceUpdateBillingTier(workspaceId);

            String invoiceId = "in_" + UUID.randomUUID().toString().substring(0, 14);
            String payload = StripeTestUtils.invoicePaymentFailedEvent(
                    invoiceId, subscription.getStripeSubscriptionId(), 2999L, "usd");

            ResponseEntity<Void> response = restTemplate.exchange(
                    "/api/v1/stripe/webhook",
                    HttpMethod.POST,
                    new HttpEntity<>(payload, webhookHeaders(payload)),
                    Void.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            Subscription updated = subscriptionRepository.findByStripeSubscriptionId(
                    subscription.getStripeSubscriptionId());
            assertThat(updated.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
        }
    }

    @Nested
    @DisplayName("Payment Intent Events")
    class PaymentIntentEvents {

        @Test
        @DisplayName("Should update payment to succeeded on payment_intent.succeeded")
        void shouldUpdatePaymentOnSucceeded() {
            Plan plan = testDataBuilder.createProPlan();
            Subscription subscription = testDataBuilder.createActiveSubscription(workspaceId, plan);
            Payment payment = testDataBuilder.createPayment(subscription, PaymentStatus.PENDING);

            String payload = StripeTestUtils.paymentIntentSucceededEvent(
                    payment.getStripePaymentIntentId(), 2999L);

            ResponseEntity<Void> response = restTemplate.exchange(
                    "/api/v1/stripe/webhook",
                    HttpMethod.POST,
                    new HttpEntity<>(payload, webhookHeaders(payload)),
                    Void.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            Optional<Payment> updated = paymentRepository.findById(payment.getId());
            assertThat(updated).isPresent();
            assertThat(updated.get().getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        }

        @Test
        @DisplayName("Should update payment to failed on payment_intent.payment_failed")
        void shouldUpdatePaymentOnFailed() {
            Plan plan = testDataBuilder.createProPlan();
            Subscription subscription = testDataBuilder.createActiveSubscription(workspaceId, plan);
            Payment payment = testDataBuilder.createPayment(subscription, PaymentStatus.PENDING);

            String payload = StripeTestUtils.paymentIntentFailedEvent(payment.getStripePaymentIntentId());

            ResponseEntity<Void> response = restTemplate.exchange(
                    "/api/v1/stripe/webhook",
                    HttpMethod.POST,
                    new HttpEntity<>(payload, webhookHeaders(payload)),
                    Void.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            Optional<Payment> updated = paymentRepository.findById(payment.getId());
            assertThat(updated).isPresent();
            assertThat(updated.get().getStatus()).isEqualTo(PaymentStatus.FAILED);
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("Should return 400 for invalid signature")
        void shouldReturn400ForInvalidSignature() {
            String payload = StripeTestUtils.checkoutSessionCompletedEvent(
                    "cs_test", workspaceId, UUID.randomUUID());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Stripe-Signature", "t=123,v1=invalid_signature");

            ResponseEntity<Void> response = restTemplate.exchange(
                    "/api/v1/stripe/webhook",
                    HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    Void.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should return 400 for missing signature header")
        void shouldReturn400ForMissingSignature() {
            String payload = StripeTestUtils.checkoutSessionCompletedEvent(
                    "cs_test", workspaceId, UUID.randomUUID());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Void> response = restTemplate.exchange(
                    "/api/v1/stripe/webhook",
                    HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    Void.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should return 200 for unknown event type (graceful ignore)")
        void shouldReturn200ForUnknownEventType() {
            String payload = """
                {
                    "id": "evt_test",
                    "object": "event",
                    "type": "unknown.event.type",
                    "data": {
                        "object": {}
                    }
                }
                """;

            ResponseEntity<Void> response = restTemplate.exchange(
                    "/api/v1/stripe/webhook",
                    HttpMethod.POST,
                    new HttpEntity<>(payload, webhookHeaders(payload)),
                    Void.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }
}
