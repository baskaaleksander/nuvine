package com.baskaaleksander.nuvine.integration.controller;

import com.baskaaleksander.nuvine.application.dto.CustomerPortalSessionRequest;
import com.baskaaleksander.nuvine.application.dto.CustomerPortalSessionResponse;
import com.baskaaleksander.nuvine.application.dto.PaymentSessionRequest;
import com.baskaaleksander.nuvine.application.dto.PaymentSessionResponse;
import com.baskaaleksander.nuvine.domain.model.*;
import com.baskaaleksander.nuvine.integration.base.BaseControllerIntegrationTest;
import com.baskaaleksander.nuvine.integration.support.JwtTestUtils;
import com.baskaaleksander.nuvine.integration.support.TestDataBuilder;
import com.baskaaleksander.nuvine.integration.support.WireMockStubs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionControllerIT extends BaseControllerIntegrationTest {

    @Autowired
    private TestDataBuilder testDataBuilder;

    private WireMockStubs wireMockStubs;
    private JwtTestUtils jwtTestUtils;

    private UUID userId;
    private UUID workspaceId;
    private String userJwt;

    @BeforeEach
    void setUp() {
        testDataBuilder.cleanUp();
        wireMockStubs = new WireMockStubs(wireMockServer);
        jwtTestUtils = new JwtTestUtils("http://localhost:" + wireMockServer.port() + "/realms/nuvine");

        userId = UUID.randomUUID();
        workspaceId = UUID.randomUUID();
        userJwt = jwtTestUtils.generateJwt(userId, "user@example.com");
    }

    @Nested
    @DisplayName("POST /api/v1/subscription/payment-session")
    class CreatePaymentSession {

        @Test
        @DisplayName("Should return existing pending session if available")
        void shouldReturnExistingPendingSession() {
            Plan plan = testDataBuilder.createProPlan();
            PaymentSession existingSession = testDataBuilder.createPaymentSession(
                    workspaceId, userId, plan, PaymentSessionStatus.PENDING);

            wireMockStubs.stubWorkspaceServiceGetWorkspace(workspaceId, userId, "Test Workspace", "FREE");

            PaymentSessionRequest request = new PaymentSessionRequest(
                    workspaceId,
                    plan.getId(),
                    PaymentSessionIntent.SUBSCRIPTION_CREATE
            );

            HttpHeaders headers = authHeaders(userJwt);
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<PaymentSessionResponse> response = restTemplate.exchange(
                    "/api/v1/subscription/payment-session",
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    PaymentSessionResponse.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().sessionId()).isEqualTo(existingSession.getStripeSessionId());
        }

        @Test
        @DisplayName("Should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticated() {
            Plan plan = testDataBuilder.createProPlan();

            PaymentSessionRequest request = new PaymentSessionRequest(
                    workspaceId,
                    plan.getId(),
                    PaymentSessionIntent.SUBSCRIPTION_CREATE
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/subscription/payment-session",
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Should return 404 for non-existent plan")
        void shouldReturn404ForNonExistentPlan() {
            wireMockStubs.stubWorkspaceServiceGetWorkspace(workspaceId, userId, "Test Workspace", "FREE");

            PaymentSessionRequest request = new PaymentSessionRequest(
                    workspaceId,
                    UUID.randomUUID(),
                    PaymentSessionIntent.SUBSCRIPTION_CREATE
            );

            HttpHeaders headers = authHeaders(userJwt);
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/subscription/payment-session",
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/subscription/customer-portal-session")
    class CreateCustomerPortalSession {

        @Test
        @DisplayName("Should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticated() {
            CustomerPortalSessionRequest request = new CustomerPortalSessionRequest(workspaceId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/subscription/customer-portal-session",
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Should return 404 for workspace without subscription")
        void shouldReturn404ForWorkspaceWithoutSubscription() {
            wireMockStubs.stubWorkspaceServiceGetWorkspace(workspaceId, userId, "Test Workspace", "FREE");

            CustomerPortalSessionRequest request = new CustomerPortalSessionRequest(workspaceId);

            HttpHeaders headers = authHeaders(userJwt);
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/subscription/customer-portal-session",
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
