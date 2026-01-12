package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.TestFixtures;
import com.baskaaleksander.nuvine.application.dto.CustomerPortalSessionResponse;
import com.baskaaleksander.nuvine.application.dto.PaymentSessionResponse;
import com.baskaaleksander.nuvine.application.dto.UserInternalResponse;
import com.baskaaleksander.nuvine.application.dto.WorkspaceInternalSubscriptionResponse;
import com.baskaaleksander.nuvine.domain.exception.ForbiddenAccessException;
import com.baskaaleksander.nuvine.domain.exception.SubscriptionConflictException;
import com.baskaaleksander.nuvine.domain.exception.SubscriptionNotFoundException;
import com.baskaaleksander.nuvine.domain.model.*;
import com.baskaaleksander.nuvine.domain.model.Plan;
import com.baskaaleksander.nuvine.domain.model.Subscription;
import com.baskaaleksander.nuvine.infrastructure.client.AuthServiceCacheWrapper;
import com.baskaaleksander.nuvine.infrastructure.client.WorkspaceServiceCacheWrapper;
import com.baskaaleksander.nuvine.infrastructure.persistence.PaymentSessionRepository;
import com.baskaaleksander.nuvine.infrastructure.persistence.SubscriptionRepository;
import com.stripe.StripeClient;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerSearchParams;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.service.*;
import com.stripe.service.SubscriptionService;
import com.stripe.service.billingportal.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionService")
class SubscriptionServiceTest {

    @Mock
    private StripeClient stripeClient;

    @Mock
    private PlanService planService;

    @Mock
    private SubscriptionCacheService subscriptionCacheService;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private AuthServiceCacheWrapper authServiceCacheWrapper;

    @Mock
    private WorkspaceServiceCacheWrapper workspaceServiceCacheWrapper;

    @Mock
    private PaymentSessionRepository paymentSessionRepository;

    @InjectMocks
    private com.baskaaleksander.nuvine.domain.service.SubscriptionService subscriptionService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(subscriptionService, "successUrl", "https://example.com/success");
        ReflectionTestUtils.setField(subscriptionService, "cancelUrl", "https://example.com/cancel");
    }

    @Nested
    @DisplayName("createCustomerPortalSession")
    class CreateCustomerPortalSession {

        @Test
        @DisplayName("should create portal session for workspace owner")
        void createCustomerPortalSession_validOwner_returnsPortalUrl() throws Exception {
            UUID workspaceId = TestFixtures.DEFAULT_WORKSPACE_ID;
            UUID userId = TestFixtures.DEFAULT_USER_ID;
            String expectedUrl = "https://billing.stripe.com/session/test";

            WorkspaceInternalSubscriptionResponse workspace = TestFixtures.workspaceInternalResponse(userId);
            UserInternalResponse user = TestFixtures.userInternalResponse(userId);

            when(workspaceServiceCacheWrapper.getWorkspaceSubscription(workspaceId)).thenReturn(workspace);
            when(authServiceCacheWrapper.getUserInternalResponse(userId)).thenReturn(user);

            Subscription subscription = TestFixtures.activeSubscription()
                    .workspaceId(workspaceId)
                    .build();
            when(subscriptionCacheService.findByWorkspaceId(workspaceId)).thenReturn(Optional.of(subscription));

            V1Services v1Services = mock(V1Services.class);
            BillingPortalService billingPortalService = mock(BillingPortalService.class);
            SessionService sessionService = mock(SessionService.class);
            com.stripe.model.billingportal.Session portalSession = mock(com.stripe.model.billingportal.Session.class);

            when(stripeClient.v1()).thenReturn(v1Services);
            when(v1Services.billingPortal()).thenReturn(billingPortalService);
            when(billingPortalService.sessions()).thenReturn(sessionService);
            when(sessionService.create(any())).thenReturn(portalSession);
            when(portalSession.getUrl()).thenReturn(expectedUrl);

            CustomerPortalSessionResponse response = subscriptionService.createCustomerPortalSession(workspaceId, userId);

            assertThat(response.url()).isEqualTo(expectedUrl);
        }

        @Test
        @DisplayName("should throw ForbiddenAccessException when user is not owner")
        void createCustomerPortalSession_notOwner_throwsForbiddenAccessException() {
            UUID workspaceId = TestFixtures.DEFAULT_WORKSPACE_ID;
            UUID userId = TestFixtures.DEFAULT_USER_ID;
            UUID otherUserId = TestFixtures.randomId();

            WorkspaceInternalSubscriptionResponse workspace = TestFixtures.workspaceInternalResponse(otherUserId);
            UserInternalResponse user = TestFixtures.userInternalResponse(userId);

            when(workspaceServiceCacheWrapper.getWorkspaceSubscription(workspaceId)).thenReturn(workspace);
            when(authServiceCacheWrapper.getUserInternalResponse(userId)).thenReturn(user);

            assertThatThrownBy(() -> subscriptionService.createCustomerPortalSession(workspaceId, userId))
                    .isInstanceOf(ForbiddenAccessException.class)
                    .hasMessageContaining("not the owner");
        }

        @Test
        @DisplayName("should throw SubscriptionNotFoundException when no subscription exists")
        void createCustomerPortalSession_noSubscription_throwsException() {
            UUID workspaceId = TestFixtures.DEFAULT_WORKSPACE_ID;
            UUID userId = TestFixtures.DEFAULT_USER_ID;

            WorkspaceInternalSubscriptionResponse workspace = TestFixtures.workspaceInternalResponse(userId);
            UserInternalResponse user = TestFixtures.userInternalResponse(userId);

            when(workspaceServiceCacheWrapper.getWorkspaceSubscription(workspaceId)).thenReturn(workspace);
            when(authServiceCacheWrapper.getUserInternalResponse(userId)).thenReturn(user);
            when(subscriptionCacheService.findByWorkspaceId(workspaceId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> subscriptionService.createCustomerPortalSession(workspaceId, userId))
                    .isInstanceOf(SubscriptionNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("createPaymentSession - SUBSCRIPTION_CREATE")
    class CreatePaymentSessionForCreate {

        @Test
        @DisplayName("should return existing pending session if available")
        void createPaymentSession_existingPendingSession_returnsExistingSession() {
            UUID workspaceId = TestFixtures.DEFAULT_WORKSPACE_ID;
            UUID planId = TestFixtures.DEFAULT_PLAN_ID;
            UUID userId = TestFixtures.DEFAULT_USER_ID;
            String existingUrl = "https://checkout.stripe.com/existing";
            String existingSessionId = "cs_existing";

            PaymentSession existingSession = TestFixtures.paymentSession()
                    .workspaceId(workspaceId)
                    .planId(planId)
                    .userId(userId)
                    .stripeUrl(existingUrl)
                    .stripeSessionId(existingSessionId)
                    .build();

            when(paymentSessionRepository.findValidSession(
                    eq(workspaceId), eq(planId), any(), eq(PaymentSessionIntent.SUBSCRIPTION_CREATE),
                    eq(userId), any(Instant.class), eq(PaymentSessionStatus.PENDING)))
                    .thenReturn(Optional.of(existingSession));

            PaymentSessionResponse response = subscriptionService.createPaymentSession(
                    workspaceId, planId, PaymentSessionIntent.SUBSCRIPTION_CREATE, userId);

            assertThat(response.url()).isEqualTo(existingUrl);
            assertThat(response.sessionId()).isEqualTo(existingSessionId);
            verify(stripeClient, never()).v1();
        }

        @Test
        @DisplayName("should throw SubscriptionConflictException when subscription already exists")
        void createPaymentSession_subscriptionExists_throwsConflictException() {
            UUID workspaceId = TestFixtures.DEFAULT_WORKSPACE_ID;
            UUID planId = TestFixtures.DEFAULT_PLAN_ID;
            UUID userId = TestFixtures.DEFAULT_USER_ID;

            when(paymentSessionRepository.findValidSession(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(Optional.empty());

            Plan plan = TestFixtures.proPlan().id(planId).build();
            when(planService.findById(planId)).thenReturn(Optional.of(plan));

            Subscription existingSubscription = TestFixtures.activeSubscription().build();
            when(subscriptionCacheService.findByWorkspaceId(workspaceId)).thenReturn(Optional.of(existingSubscription));

            assertThatThrownBy(() -> subscriptionService.createPaymentSession(
                    workspaceId, planId, PaymentSessionIntent.SUBSCRIPTION_CREATE, userId))
                    .isInstanceOf(SubscriptionConflictException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("should create new checkout session for new subscription")
        void createPaymentSession_newSubscription_createsCheckoutSession() throws Exception {
            UUID workspaceId = TestFixtures.DEFAULT_WORKSPACE_ID;
            UUID planId = TestFixtures.DEFAULT_PLAN_ID;
            UUID userId = TestFixtures.DEFAULT_USER_ID;
            String expectedUrl = "https://checkout.stripe.com/new";
            String expectedSessionId = "cs_new";

            when(paymentSessionRepository.findValidSession(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(Optional.empty());

            Plan plan = TestFixtures.proPlan().id(planId).build();
            when(planService.findById(planId)).thenReturn(Optional.of(plan));
            when(subscriptionCacheService.findByWorkspaceId(workspaceId)).thenReturn(Optional.empty());

            WorkspaceInternalSubscriptionResponse workspace = TestFixtures.workspaceInternalResponse(userId);
            UserInternalResponse user = TestFixtures.userInternalResponse(userId);
            when(workspaceServiceCacheWrapper.getWorkspaceSubscription(workspaceId)).thenReturn(workspace);
            when(authServiceCacheWrapper.getUserInternalResponse(userId)).thenReturn(user);

            V1Services v1Services = mock(V1Services.class);
            CustomerService customerService = mock(CustomerService.class);
            @SuppressWarnings("unchecked")
            StripeSearchResult<Customer> searchResult = mock(StripeSearchResult.class);

            when(stripeClient.v1()).thenReturn(v1Services);
            when(v1Services.customers()).thenReturn(customerService);
            when(customerService.search(any(CustomerSearchParams.class))).thenReturn(searchResult);
            when(searchResult.getData()).thenReturn(List.of());

            Customer newCustomer = mock(Customer.class);
            when(newCustomer.getId()).thenReturn("cus_new");
            when(customerService.create(any(CustomerCreateParams.class))).thenReturn(newCustomer);

            CheckoutService checkoutService = mock(CheckoutService.class);
            com.stripe.service.checkout.SessionService checkoutSessionService = mock(com.stripe.service.checkout.SessionService.class);
            Session checkoutSession = mock(Session.class);

            when(v1Services.checkout()).thenReturn(checkoutService);
            when(checkoutService.sessions()).thenReturn(checkoutSessionService);
            when(checkoutSessionService.create(any(SessionCreateParams.class))).thenReturn(checkoutSession);
            when(checkoutSession.getUrl()).thenReturn(expectedUrl);
            when(checkoutSession.getId()).thenReturn(expectedSessionId);

            PaymentSessionResponse response = subscriptionService.createPaymentSession(
                    workspaceId, planId, PaymentSessionIntent.SUBSCRIPTION_CREATE, userId);

            assertThat(response.url()).isEqualTo(expectedUrl);
            assertThat(response.sessionId()).isEqualTo(expectedSessionId);

            ArgumentCaptor<PaymentSession> sessionCaptor = ArgumentCaptor.forClass(PaymentSession.class);
            verify(paymentSessionRepository).save(sessionCaptor.capture());

            PaymentSession savedSession = sessionCaptor.getValue();
            assertThat(savedSession.getWorkspaceId()).isEqualTo(workspaceId);
            assertThat(savedSession.getPlanId()).isEqualTo(planId);
            assertThat(savedSession.getStatus()).isEqualTo(PaymentSessionStatus.PENDING);
        }

        @Test
        @DisplayName("should throw ForbiddenAccessException when user is not workspace owner")
        void createPaymentSession_notOwner_throwsForbiddenAccessException() {
            UUID workspaceId = TestFixtures.DEFAULT_WORKSPACE_ID;
            UUID planId = TestFixtures.DEFAULT_PLAN_ID;
            UUID userId = TestFixtures.DEFAULT_USER_ID;
            UUID otherOwnerId = TestFixtures.randomId();

            when(paymentSessionRepository.findValidSession(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(Optional.empty());

            Plan plan = TestFixtures.proPlan().id(planId).build();
            when(planService.findById(planId)).thenReturn(Optional.of(plan));
            when(subscriptionCacheService.findByWorkspaceId(workspaceId)).thenReturn(Optional.empty());

            WorkspaceInternalSubscriptionResponse workspace = TestFixtures.workspaceInternalResponse(otherOwnerId);
            UserInternalResponse user = TestFixtures.userInternalResponse(userId);
            when(workspaceServiceCacheWrapper.getWorkspaceSubscription(workspaceId)).thenReturn(workspace);
            when(authServiceCacheWrapper.getUserInternalResponse(userId)).thenReturn(user);

            assertThatThrownBy(() -> subscriptionService.createPaymentSession(
                    workspaceId, planId, PaymentSessionIntent.SUBSCRIPTION_CREATE, userId))
                    .isInstanceOf(ForbiddenAccessException.class);
        }
    }

    @Nested
    @DisplayName("createPaymentSession - SUBSCRIPTION_UPDATE")
    class CreatePaymentSessionForUpdate {

        @Test
        @DisplayName("should throw SubscriptionNotFoundException when no subscription exists for update")
        void createPaymentSession_noSubscriptionForUpdate_throwsException() {
            UUID workspaceId = TestFixtures.DEFAULT_WORKSPACE_ID;
            UUID planId = TestFixtures.DEFAULT_PLAN_ID;
            UUID userId = TestFixtures.DEFAULT_USER_ID;

            when(paymentSessionRepository.findValidSession(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(Optional.empty());

            Plan plan = TestFixtures.proPlan().id(planId).build();
            when(planService.findById(planId)).thenReturn(Optional.of(plan));
            when(subscriptionCacheService.findByWorkspaceId(workspaceId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> subscriptionService.createPaymentSession(
                    workspaceId, planId, PaymentSessionIntent.SUBSCRIPTION_UPDATE, userId))
                    .isInstanceOf(SubscriptionNotFoundException.class);
        }

        @Test
        @DisplayName("should update subscription and return success url when invoice is paid")
        void createPaymentSession_updateWithPaidInvoice_returnsSuccessUrl() throws Exception {
            UUID workspaceId = TestFixtures.DEFAULT_WORKSPACE_ID;
            UUID planId = TestFixtures.DEFAULT_PLAN_ID;
            UUID userId = TestFixtures.DEFAULT_USER_ID;

            when(paymentSessionRepository.findValidSession(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(Optional.empty());

            Plan plan = TestFixtures.proPlan().id(planId).build();
            when(planService.findById(planId)).thenReturn(Optional.of(plan));

            Subscription subscription = TestFixtures.activeSubscription()
                    .workspaceId(workspaceId)
                    .stripeSubscriptionId("sub_test")
                    .build();
            when(subscriptionCacheService.findByWorkspaceId(workspaceId)).thenReturn(Optional.of(subscription));

            WorkspaceInternalSubscriptionResponse workspace = TestFixtures.workspaceInternalResponse(userId);
            UserInternalResponse user = TestFixtures.userInternalResponse(userId);
            when(workspaceServiceCacheWrapper.getWorkspaceSubscription(workspaceId)).thenReturn(workspace);
            when(authServiceCacheWrapper.getUserInternalResponse(userId)).thenReturn(user);

            V1Services v1Services = mock(V1Services.class);
            CustomerService customerService = mock(CustomerService.class);
            @SuppressWarnings("unchecked")
            StripeSearchResult<Customer> searchResult = mock(StripeSearchResult.class);

            when(stripeClient.v1()).thenReturn(v1Services);
            when(v1Services.customers()).thenReturn(customerService);
            when(customerService.search(any(CustomerSearchParams.class))).thenReturn(searchResult);

            SubscriptionService stripeSubscriptionService = mock(SubscriptionService.class);
            com.stripe.model.Subscription stripeSubscription = mock(com.stripe.model.Subscription.class);
            com.stripe.model.Subscription updatedSubscription = mock(com.stripe.model.Subscription.class);

            when(v1Services.subscriptions()).thenReturn(stripeSubscriptionService);
            when(stripeSubscriptionService.retrieve("sub_test")).thenReturn(stripeSubscription);

            SubscriptionItem subscriptionItem = mock(SubscriptionItem.class);
            when(subscriptionItem.getId()).thenReturn("si_test");
            SubscriptionItemCollection itemCollection = mock(SubscriptionItemCollection.class);
            when(itemCollection.getData()).thenReturn(List.of(subscriptionItem));
            when(stripeSubscription.getItems()).thenReturn(itemCollection);

            when(stripeSubscriptionService.update(eq("sub_test"), any(SubscriptionUpdateParams.class))).thenReturn(updatedSubscription);
            when(updatedSubscription.getLatestInvoice()).thenReturn("in_test");
            when(updatedSubscription.getId()).thenReturn("sub_updated");

            InvoiceService invoiceService = mock(InvoiceService.class);
            Invoice invoice = mock(Invoice.class);
            when(v1Services.invoices()).thenReturn(invoiceService);
            when(invoiceService.retrieve("in_test")).thenReturn(invoice);
            when(invoice.getStatus()).thenReturn("paid");

            PaymentSessionResponse response = subscriptionService.createPaymentSession(
                    workspaceId, planId, PaymentSessionIntent.SUBSCRIPTION_UPDATE, userId);

            assertThat(response.url()).isEqualTo("https://example.com/success");
            assertThat(response.sessionId()).startsWith("sub_update_sub_updated");

            ArgumentCaptor<PaymentSession> sessionCaptor = ArgumentCaptor.forClass(PaymentSession.class);
            verify(paymentSessionRepository).save(sessionCaptor.capture());
            assertThat(sessionCaptor.getValue().getStatus()).isEqualTo(PaymentSessionStatus.COMPLETED);
        }

        @Test
        @DisplayName("should return invoice url when invoice requires payment")
        void createPaymentSession_updateWithUnpaidInvoice_returnsInvoiceUrl() throws Exception {
            UUID workspaceId = TestFixtures.DEFAULT_WORKSPACE_ID;
            UUID planId = TestFixtures.DEFAULT_PLAN_ID;
            UUID userId = TestFixtures.DEFAULT_USER_ID;
            String invoiceUrl = "https://invoice.stripe.com/test";
            String invoiceId = "in_test";

            when(paymentSessionRepository.findValidSession(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(Optional.empty());

            Plan plan = TestFixtures.proPlan().id(planId).build();
            when(planService.findById(planId)).thenReturn(Optional.of(plan));

            Subscription subscription = TestFixtures.activeSubscription()
                    .workspaceId(workspaceId)
                    .stripeSubscriptionId("sub_test")
                    .build();
            when(subscriptionCacheService.findByWorkspaceId(workspaceId)).thenReturn(Optional.of(subscription));

            WorkspaceInternalSubscriptionResponse workspace = TestFixtures.workspaceInternalResponse(userId);
            UserInternalResponse user = TestFixtures.userInternalResponse(userId);
            when(workspaceServiceCacheWrapper.getWorkspaceSubscription(workspaceId)).thenReturn(workspace);
            when(authServiceCacheWrapper.getUserInternalResponse(userId)).thenReturn(user);

            V1Services v1Services = mock(V1Services.class);
            CustomerService customerService = mock(CustomerService.class);
            @SuppressWarnings("unchecked")
            StripeSearchResult<Customer> searchResult = mock(StripeSearchResult.class);

            when(stripeClient.v1()).thenReturn(v1Services);
            when(v1Services.customers()).thenReturn(customerService);
            when(customerService.search(any(CustomerSearchParams.class))).thenReturn(searchResult);

            SubscriptionService stripeSubscriptionService = mock(SubscriptionService.class);
            com.stripe.model.Subscription stripeSubscription = mock(com.stripe.model.Subscription.class);
            com.stripe.model.Subscription updatedSubscription = mock(com.stripe.model.Subscription.class);

            when(v1Services.subscriptions()).thenReturn(stripeSubscriptionService);
            when(stripeSubscriptionService.retrieve("sub_test")).thenReturn(stripeSubscription);

            SubscriptionItem subscriptionItem = mock(SubscriptionItem.class);
            when(subscriptionItem.getId()).thenReturn("si_test");
            SubscriptionItemCollection itemCollection = mock(SubscriptionItemCollection.class);
            when(itemCollection.getData()).thenReturn(List.of(subscriptionItem));
            when(stripeSubscription.getItems()).thenReturn(itemCollection);

            when(stripeSubscriptionService.update(eq("sub_test"), any(SubscriptionUpdateParams.class))).thenReturn(updatedSubscription);
            when(updatedSubscription.getLatestInvoice()).thenReturn(invoiceId);

            InvoiceService invoiceService = mock(InvoiceService.class);
            Invoice invoice = mock(Invoice.class);
            when(v1Services.invoices()).thenReturn(invoiceService);
            when(invoiceService.retrieve(invoiceId)).thenReturn(invoice);
            when(invoice.getStatus()).thenReturn("open");
            when(invoice.getHostedInvoiceUrl()).thenReturn(invoiceUrl);
            when(invoice.getId()).thenReturn(invoiceId);

            PaymentSessionResponse response = subscriptionService.createPaymentSession(
                    workspaceId, planId, PaymentSessionIntent.SUBSCRIPTION_UPDATE, userId);

            assertThat(response.url()).isEqualTo(invoiceUrl);
            assertThat(response.sessionId()).isEqualTo(invoiceId);
        }
    }
}
