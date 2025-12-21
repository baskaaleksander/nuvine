package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.TestFixtures;
import com.baskaaleksander.nuvine.application.dto.UserInternalResponse;
import com.baskaaleksander.nuvine.application.dto.WorkspaceBillingTierUpdateRequest;
import com.baskaaleksander.nuvine.application.dto.WorkspaceInternalSubscriptionResponse;
import com.baskaaleksander.nuvine.domain.model.*;
import com.baskaaleksander.nuvine.domain.model.Plan;
import com.baskaaleksander.nuvine.domain.model.Subscription;
import com.baskaaleksander.nuvine.infrastructure.client.AuthServiceClient;
import com.baskaaleksander.nuvine.infrastructure.client.WorkspaceServiceClient;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.PaymentActionRequiredEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.PaymentActionRequiredEventProducer;
import com.baskaaleksander.nuvine.infrastructure.persistence.PaymentSessionRepository;
import com.baskaaleksander.nuvine.infrastructure.persistence.PlanRepository;
import com.baskaaleksander.nuvine.infrastructure.persistence.SubscriptionRepository;
import com.stripe.StripeClient;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.service.SubscriptionService;
import com.stripe.service.V1Services;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebhookService")
class WebhookServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PaymentSessionRepository paymentSessionRepository;

    @Mock
    private WorkspaceServiceClient workspaceServiceClient;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private PaymentActionRequiredEventProducer paymentActionRequiredEventProducer;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private StripeClient stripeClient;

    @InjectMocks
    private WebhookService webhookService;

    @Nested
    @DisplayName("handleEvent - customer.subscription.created")
    class HandleCustomerSubscriptionCreated {

        @Test
        @DisplayName("should create subscription and update workspace billing tier")
        void handleEvent_subscriptionCreated_createsSubscriptionAndUpdatesBillingTier() throws Exception {
            UUID workspaceId = TestFixtures.DEFAULT_WORKSPACE_ID;
            UUID planId = TestFixtures.DEFAULT_PLAN_ID;
            String stripeCustomerId = "cus_test123";
            String stripeSubscriptionId = "sub_test123";
            Long periodStart = Instant.now().minusSeconds(86400).getEpochSecond();
            Long periodEnd = Instant.now().plusSeconds(2592000).getEpochSecond();

            Event event = createMockEvent("customer.subscription.created");
            com.stripe.model.Subscription stripeSubscription = mock(com.stripe.model.Subscription.class);
            setupEventDeserializer(event, stripeSubscription);

            when(stripeSubscription.getMetadata()).thenReturn(Map.of(
                    "workspace_id", workspaceId.toString(),
                    "plan_id", planId.toString()
            ));
            when(stripeSubscription.getCustomer()).thenReturn(stripeCustomerId);
            when(stripeSubscription.getId()).thenReturn(stripeSubscriptionId);
            when(stripeSubscription.getCancelAtPeriodEnd()).thenReturn(false);

            SubscriptionItem subscriptionItem = mock(SubscriptionItem.class);
            when(subscriptionItem.getCurrentPeriodStart()).thenReturn(periodStart);
            when(subscriptionItem.getCurrentPeriodEnd()).thenReturn(periodEnd);

            SubscriptionItemCollection itemCollection = mock(SubscriptionItemCollection.class);
            when(itemCollection.getData()).thenReturn(List.of(subscriptionItem));
            when(stripeSubscription.getItems()).thenReturn(itemCollection);

            Plan plan = TestFixtures.proPlan().id(planId).build();
            when(planRepository.findById(planId)).thenReturn(Optional.of(plan));

            webhookService.handleEvent(event);

            ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
            verify(subscriptionRepository).save(subscriptionCaptor.capture());

            Subscription savedSubscription = subscriptionCaptor.getValue();
            assertThat(savedSubscription.getWorkspaceId()).isEqualTo(workspaceId);
            assertThat(savedSubscription.getPlanId()).isEqualTo(planId);
            assertThat(savedSubscription.getStripeCustomerId()).isEqualTo(stripeCustomerId);
            assertThat(savedSubscription.getStripeSubscriptionId()).isEqualTo(stripeSubscriptionId);
            assertThat(savedSubscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);

            verify(workspaceServiceClient).updateWorkspaceBillingTier(
                    eq(workspaceId),
                    any(WorkspaceBillingTierUpdateRequest.class)
            );
        }
    }

    @Nested
    @DisplayName("handleEvent - customer.subscription.updated")
    class HandleCustomerSubscriptionUpdated {

        @Test
        @DisplayName("should update existing subscription status and period")
        void handleEvent_subscriptionUpdated_updatesSubscription() throws Exception {
            String stripeSubscriptionId = "sub_test123";
            UUID planId = TestFixtures.DEFAULT_PLAN_ID;
            UUID workspaceId = TestFixtures.DEFAULT_WORKSPACE_ID;
            Long periodStart = Instant.now().getEpochSecond();
            Long periodEnd = Instant.now().plusSeconds(2592000).getEpochSecond();

            Event event = createMockEvent("customer.subscription.updated");
            com.stripe.model.Subscription stripeSubscription = mock(com.stripe.model.Subscription.class);
            setupEventDeserializer(event, stripeSubscription);

            when(stripeSubscription.getId()).thenReturn(stripeSubscriptionId);
            when(stripeSubscription.getCancelAtPeriodEnd()).thenReturn(true);
            when(stripeSubscription.getStatus()).thenReturn("active");
            when(stripeSubscription.getMetadata()).thenReturn(Map.of(
                    "plan_id", planId.toString(),
                    "workspace_id", workspaceId.toString()
            ));

            SubscriptionItem subscriptionItem = mock(SubscriptionItem.class);
            when(subscriptionItem.getCurrentPeriodStart()).thenReturn(periodStart);
            when(subscriptionItem.getCurrentPeriodEnd()).thenReturn(periodEnd);

            SubscriptionItemCollection itemCollection = mock(SubscriptionItemCollection.class);
            when(itemCollection.getData()).thenReturn(List.of(subscriptionItem));
            when(stripeSubscription.getItems()).thenReturn(itemCollection);

            Subscription existingSubscription = TestFixtures.activeSubscription()
                    .stripeSubscriptionId(stripeSubscriptionId)
                    .build();
            when(subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId))
                    .thenReturn(existingSubscription);

            Plan plan = TestFixtures.proPlan().id(planId).build();
            when(planRepository.findById(planId)).thenReturn(Optional.of(plan));

            webhookService.handleEvent(event);

            verify(subscriptionRepository).save(existingSubscription);
            assertThat(existingSubscription.getCancelAtPeriodEnd()).isTrue();
            assertThat(existingSubscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(existingSubscription.getPlanId()).isEqualTo(planId);

            verify(workspaceServiceClient).updateWorkspaceBillingTier(
                    eq(existingSubscription.getWorkspaceId()),
                    any(WorkspaceBillingTierUpdateRequest.class)
            );
        }

        @Test
        @DisplayName("should not update billing tier when subscription not found")
        void handleEvent_subscriptionNotFound_doesNotUpdate() throws Exception {
            String stripeSubscriptionId = "sub_nonexistent";

            Event event = createMockEvent("customer.subscription.updated");
            com.stripe.model.Subscription stripeSubscription = mock(com.stripe.model.Subscription.class);
            setupEventDeserializer(event, stripeSubscription);

            when(stripeSubscription.getId()).thenReturn(stripeSubscriptionId);
            when(subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId))
                    .thenReturn(null);

            webhookService.handleEvent(event);

            verify(subscriptionRepository, never()).save(any());
            verify(workspaceServiceClient, never()).updateWorkspaceBillingTier(any(), any());
        }
    }

    @Nested
    @DisplayName("handleEvent - customer.subscription.deleted")
    class HandleCustomerSubscriptionDeleted {

        @Test
        @DisplayName("should mark subscription as deleted and update workspace to free tier")
        void handleEvent_subscriptionDeleted_marksDeletedAndUpdatesWorkspace() throws Exception {
            String stripeSubscriptionId = "sub_test123";

            Event event = createMockEvent("customer.subscription.deleted");
            com.stripe.model.Subscription stripeSubscription = mock(com.stripe.model.Subscription.class);
            setupEventDeserializer(event, stripeSubscription);

            when(stripeSubscription.getId()).thenReturn(stripeSubscriptionId);

            Subscription existingSubscription = TestFixtures.activeSubscription()
                    .stripeSubscriptionId(stripeSubscriptionId)
                    .build();
            when(subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId))
                    .thenReturn(existingSubscription);

            webhookService.handleEvent(event);

            verify(subscriptionRepository).save(existingSubscription);
            assertThat(existingSubscription.getStatus()).isEqualTo(SubscriptionStatus.DELETED);

            ArgumentCaptor<WorkspaceBillingTierUpdateRequest> requestCaptor =
                    ArgumentCaptor.forClass(WorkspaceBillingTierUpdateRequest.class);
            verify(workspaceServiceClient).updateWorkspaceBillingTier(
                    eq(existingSubscription.getWorkspaceId()),
                    requestCaptor.capture()
            );
            assertThat(requestCaptor.getValue().billingTierCode()).isEqualTo("FREE");
        }
    }

    @Nested
    @DisplayName("handleEvent - invoice.payment_failed")
    class HandleInvoicePaymentFailed {

        @Test
        @DisplayName("should update subscription to past_due and downgrade to free tier")
        void handleEvent_invoicePaymentFailed_updatesSubscriptionToPastDue() throws Exception {
            String stripeSubscriptionId = "sub_test123";

            Event event = createMockEvent("invoice.payment_failed");
            com.stripe.model.Invoice stripeInvoice = mock(com.stripe.model.Invoice.class);
            setupEventDeserializer(event, stripeInvoice);

            Invoice.Parent parent = mock(Invoice.Parent.class);
            Invoice.Parent.SubscriptionDetails subscriptionDetails = mock(Invoice.Parent.SubscriptionDetails.class);
            when(stripeInvoice.getParent()).thenReturn(parent);
            when(parent.getSubscriptionDetails()).thenReturn(subscriptionDetails);
            when(subscriptionDetails.getSubscription()).thenReturn(stripeSubscriptionId);

            Subscription existingSubscription = TestFixtures.activeSubscription()
                    .stripeSubscriptionId(stripeSubscriptionId)
                    .build();
            when(subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId))
                    .thenReturn(existingSubscription);

            webhookService.handleEvent(event);

            verify(subscriptionRepository).save(existingSubscription);
            assertThat(existingSubscription.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);

            ArgumentCaptor<WorkspaceBillingTierUpdateRequest> requestCaptor =
                    ArgumentCaptor.forClass(WorkspaceBillingTierUpdateRequest.class);
            verify(workspaceServiceClient).updateWorkspaceBillingTier(
                    eq(existingSubscription.getWorkspaceId()),
                    requestCaptor.capture()
            );
            assertThat(requestCaptor.getValue().billingTierCode()).isEqualTo("FREE");
        }
    }

    @Nested
    @DisplayName("handleEvent - invoice.paid")
    class HandleInvoicePaid {

        @Test
        @DisplayName("should restore subscription to active when previously past_due")
        void handleEvent_invoicePaid_restoresActiveSubscription() throws Exception {
            String stripeSubscriptionId = "sub_test123";
            UUID planId = TestFixtures.DEFAULT_PLAN_ID;
            Long periodStart = Instant.now().getEpochSecond();
            Long periodEnd = Instant.now().plusSeconds(2592000).getEpochSecond();

            Event event = createMockEvent("invoice.paid");
            com.stripe.model.Invoice stripeInvoice = mock(com.stripe.model.Invoice.class);
            setupEventDeserializer(event, stripeInvoice);

            Invoice.Parent parent = mock(Invoice.Parent.class);
            Invoice.Parent.SubscriptionDetails subscriptionDetails = mock(Invoice.Parent.SubscriptionDetails.class);
            when(stripeInvoice.getParent()).thenReturn(parent);
            when(parent.getSubscriptionDetails()).thenReturn(subscriptionDetails);
            when(subscriptionDetails.getSubscription()).thenReturn(stripeSubscriptionId);

            Subscription existingSubscription = TestFixtures.pastDueSubscription()
                    .planId(planId)
                    .stripeSubscriptionId(stripeSubscriptionId)
                    .build();
            when(subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId))
                    .thenReturn(existingSubscription);

            Plan plan = TestFixtures.proPlan().id(planId).build();
            when(planRepository.findById(planId)).thenReturn(Optional.of(plan));

            V1Services v1Services = mock(V1Services.class);
            SubscriptionService subscriptionService = mock(SubscriptionService.class);
            com.stripe.model.Subscription stripeSubscription = mock(com.stripe.model.Subscription.class);

            when(stripeClient.v1()).thenReturn(v1Services);
            when(v1Services.subscriptions()).thenReturn(subscriptionService);
            when(subscriptionService.retrieve(stripeSubscriptionId)).thenReturn(stripeSubscription);

            SubscriptionItem subscriptionItem = mock(SubscriptionItem.class);
            when(subscriptionItem.getCurrentPeriodStart()).thenReturn(periodStart);
            when(subscriptionItem.getCurrentPeriodEnd()).thenReturn(periodEnd);

            SubscriptionItemCollection itemCollection = mock(SubscriptionItemCollection.class);
            when(itemCollection.getData()).thenReturn(List.of(subscriptionItem));
            when(stripeSubscription.getItems()).thenReturn(itemCollection);

            webhookService.handleEvent(event);

            verify(subscriptionRepository).save(existingSubscription);
            assertThat(existingSubscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);

            verify(workspaceServiceClient).updateWorkspaceBillingTier(
                    eq(existingSubscription.getWorkspaceId()),
                    any(WorkspaceBillingTierUpdateRequest.class)
            );
        }
    }

    @Nested
    @DisplayName("handleEvent - invoice.payment_action_required")
    class HandleInvoicePaymentActionRequired {

        @Test
        @DisplayName("should send payment action required notification")
        void handleEvent_invoicePaymentActionRequired_sendsNotification() throws Exception {
            String stripeSubscriptionId = "sub_test123";
            String invoiceId = "in_test123";
            String hostedInvoiceUrl = "https://invoice.stripe.com/test";

            Event event = createMockEvent("invoice.payment_action_required");
            com.stripe.model.Invoice stripeInvoice = mock(com.stripe.model.Invoice.class);
            setupEventDeserializer(event, stripeInvoice);

            when(stripeInvoice.getId()).thenReturn(invoiceId);
            when(stripeInvoice.getHostedInvoiceUrl()).thenReturn(hostedInvoiceUrl);

            Invoice.Parent parent = mock(Invoice.Parent.class);
            Invoice.Parent.SubscriptionDetails subscriptionDetails = mock(Invoice.Parent.SubscriptionDetails.class);
            when(stripeInvoice.getParent()).thenReturn(parent);
            when(parent.getSubscriptionDetails()).thenReturn(subscriptionDetails);
            when(subscriptionDetails.getSubscription()).thenReturn(stripeSubscriptionId);

            Subscription existingSubscription = TestFixtures.activeSubscription()
                    .stripeSubscriptionId(stripeSubscriptionId)
                    .build();
            when(subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId))
                    .thenReturn(existingSubscription);

            WorkspaceInternalSubscriptionResponse workspace = TestFixtures.workspaceInternalResponse();
            UserInternalResponse owner = TestFixtures.userInternalResponse(workspace.ownerId());

            when(workspaceServiceClient.getWorkspaceSubscription(existingSubscription.getWorkspaceId()))
                    .thenReturn(workspace);
            when(authServiceClient.getUserInternalResponse(workspace.ownerId()))
                    .thenReturn(owner);

            webhookService.handleEvent(event);

            verify(subscriptionRepository).save(existingSubscription);
            assertThat(existingSubscription.getStatus()).isEqualTo(SubscriptionStatus.INCOMPLETE);

            ArgumentCaptor<PaymentActionRequiredEvent> eventCaptor =
                    ArgumentCaptor.forClass(PaymentActionRequiredEvent.class);
            verify(paymentActionRequiredEventProducer).producePaymentActionRequiredEvent(eventCaptor.capture());

            PaymentActionRequiredEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.ownerEmail()).isEqualTo(owner.email());
            assertThat(capturedEvent.invoiceId()).isEqualTo(invoiceId);
            assertThat(capturedEvent.invoiceUrl()).isEqualTo(hostedInvoiceUrl);
        }
    }

    @Nested
    @DisplayName("handleEvent - checkout.session.completed")
    class HandleCheckoutSessionCompleted {

        @Test
        @DisplayName("should mark payment session as completed")
        void handleEvent_checkoutSessionCompleted_marksSessionCompleted() throws Exception {
            String stripeSessionId = "cs_test123";

            Event event = createMockEvent("checkout.session.completed");
            Session checkoutSession = mock(Session.class);
            setupEventDeserializer(event, checkoutSession);

            when(checkoutSession.getId()).thenReturn(stripeSessionId);

            PaymentSession paymentSession = TestFixtures.paymentSession()
                    .stripeSessionId(stripeSessionId)
                    .build();
            when(paymentSessionRepository.findByStripeSessionId(stripeSessionId))
                    .thenReturn(Optional.of(paymentSession));

            webhookService.handleEvent(event);

            verify(paymentSessionRepository).save(paymentSession);
            assertThat(paymentSession.getStatus()).isEqualTo(PaymentSessionStatus.COMPLETED);
            assertThat(paymentSession.getCompletedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("handleEvent - checkout.session.expired")
    class HandleCheckoutSessionExpired {

        @Test
        @DisplayName("should mark payment session as expired")
        void handleEvent_checkoutSessionExpired_marksSessionExpired() throws Exception {
            String stripeSessionId = "cs_test123";

            Event event = createMockEvent("checkout.session.expired");
            Session checkoutSession = mock(Session.class);
            setupEventDeserializer(event, checkoutSession);

            when(checkoutSession.getId()).thenReturn(stripeSessionId);

            PaymentSession paymentSession = TestFixtures.paymentSession()
                    .stripeSessionId(stripeSessionId)
                    .build();
            when(paymentSessionRepository.findByStripeSessionId(stripeSessionId))
                    .thenReturn(Optional.of(paymentSession));

            webhookService.handleEvent(event);

            verify(paymentSessionRepository).save(paymentSession);
            assertThat(paymentSession.getStatus()).isEqualTo(PaymentSessionStatus.EXPIRED);
            assertThat(paymentSession.getCompletedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("handleEvent - unknown event type")
    class HandleUnknownEventType {

        @Test
        @DisplayName("should handle unknown event type gracefully")
        void handleEvent_unknownEventType_logsWarning() {
            Event event = mock(Event.class);
            when(event.getType()).thenReturn("unknown.event.type");

            webhookService.handleEvent(event);

            verify(subscriptionRepository, never()).save(any());
            verify(paymentSessionRepository, never()).save(any());
        }
    }


    private Event createMockEvent(String eventType) {
        Event event = mock(Event.class);
        when(event.getType()).thenReturn(eventType);
        return event;
    }

    @SuppressWarnings("unchecked")
    private <T extends StripeObject> void setupEventDeserializer(Event event, T object) {
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(object));
    }
}
