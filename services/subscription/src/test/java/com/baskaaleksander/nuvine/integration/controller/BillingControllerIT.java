package com.baskaaleksander.nuvine.integration.controller;

import com.baskaaleksander.nuvine.application.dto.*;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BillingControllerIT extends BaseControllerIntegrationTest {

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
    @DisplayName("GET /api/v1/billing/models/pricing")
    class GetModelPricing {

        @Test
        @DisplayName("Should return active model pricing (public endpoint)")
        void shouldReturnActiveModelPricing() {
            LlmProvider provider = testDataBuilder.createLlmProvider("openai", "OpenAI");
            testDataBuilder.createLlmModel(provider, "gpt-4", "GPT-4");

            ResponseEntity<List<ModelPricingResponse>> response = restTemplate.exchange(
                    "/api/v1/billing/models/pricing",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/billing/workspaces/{workspaceId}/subscription")
    class GetSubscriptionStatus {

        @Test
        @DisplayName("Should return subscription status for workspace owner")
        void shouldReturnSubscriptionStatusForOwner() {
            Plan plan = testDataBuilder.createProPlan();
            testDataBuilder.createActiveSubscription(workspaceId, plan);
            wireMockStubs.stubWorkspaceServiceGetWorkspaceMember(workspaceId, userId, "OWNER");

            ResponseEntity<SubscriptionStatusResponse> response = restTemplate.exchange(
                    "/api/v1/billing/workspaces/{workspaceId}/subscription",
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders(userJwt)),
                    SubscriptionStatusResponse.class,
                    workspaceId
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
        }

        @Test
        @DisplayName("Should return 403 for non-owner")
        void shouldReturn403ForNonOwner() {
            Plan plan = testDataBuilder.createProPlan();
            testDataBuilder.createActiveSubscription(workspaceId, plan);
            wireMockStubs.stubWorkspaceServiceGetWorkspaceMember(workspaceId, userId, "MEMBER");

            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/billing/workspaces/{workspaceId}/subscription",
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders(userJwt)),
                    String.class,
                    workspaceId
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("Should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticated() {
            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/billing/workspaces/{workspaceId}/subscription",
                    HttpMethod.GET,
                    null,
                    String.class,
                    workspaceId
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/billing/workspaces/{workspaceId}/usage-logs")
    class GetUsageLogs {

        @Test
        @DisplayName("Should return paginated usage logs for owner")
        void shouldReturnPaginatedUsageLogs() {
            Plan plan = testDataBuilder.createProPlan();
            Subscription subscription = testDataBuilder.createActiveSubscription(workspaceId, plan);
            testDataBuilder.createUsageLog(subscription, userId, "gpt-4", 100, 200, new BigDecimal("0.05"));
            testDataBuilder.createUsageLog(subscription, userId, "gpt-4", 150, 250, new BigDecimal("0.07"));
            wireMockStubs.stubWorkspaceServiceGetWorkspaceMember(workspaceId, userId, "OWNER");

            ResponseEntity<PagedResponse<UsageLogResponse>> response = restTemplate.exchange(
                    "/api/v1/billing/workspaces/{workspaceId}/usage-logs?page=0&size=10",
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders(userJwt)),
                    new ParameterizedTypeReference<>() {},
                    workspaceId
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().content()).hasSize(2);
        }

        @Test
        @DisplayName("Should return 403 for non-owner")
        void shouldReturn403ForNonOwner() {
            wireMockStubs.stubWorkspaceServiceGetWorkspaceMember(workspaceId, userId, "MEMBER");

            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/billing/workspaces/{workspaceId}/usage-logs",
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders(userJwt)),
                    String.class,
                    workspaceId
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/billing/workspaces/{workspaceId}/payments")
    class GetPayments {

        @Test
        @DisplayName("Should return paginated payments for owner")
        void shouldReturnPaginatedPayments() {
            Plan plan = testDataBuilder.createProPlan();
            Subscription subscription = testDataBuilder.createActiveSubscription(workspaceId, plan);
            testDataBuilder.createPayment(subscription, PaymentStatus.SUCCEEDED);
            testDataBuilder.createPayment(subscription, PaymentStatus.PENDING);
            wireMockStubs.stubWorkspaceServiceGetWorkspaceMember(workspaceId, userId, "OWNER");

            ResponseEntity<PagedResponse<PaymentResponse>> response = restTemplate.exchange(
                    "/api/v1/billing/workspaces/{workspaceId}/payments?page=0&size=10",
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders(userJwt)),
                    new ParameterizedTypeReference<>() {},
                    workspaceId
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().content()).hasSize(2);
        }

        @Test
        @DisplayName("Should filter payments by status")
        void shouldFilterPaymentsByStatus() {
            Plan plan = testDataBuilder.createProPlan();
            Subscription subscription = testDataBuilder.createActiveSubscription(workspaceId, plan);
            testDataBuilder.createPayment(subscription, PaymentStatus.SUCCEEDED);
            testDataBuilder.createPayment(subscription, PaymentStatus.PENDING);
            wireMockStubs.stubWorkspaceServiceGetWorkspaceMember(workspaceId, userId, "OWNER");

            ResponseEntity<PagedResponse<PaymentResponse>> response = restTemplate.exchange(
                    "/api/v1/billing/workspaces/{workspaceId}/payments?page=0&size=10&status=SUCCEEDED",
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders(userJwt)),
                    new ParameterizedTypeReference<>() {},
                    workspaceId
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().content()).hasSize(1);
        }

        @Test
        @DisplayName("Should return 403 for non-owner")
        void shouldReturn403ForNonOwner() {
            wireMockStubs.stubWorkspaceServiceGetWorkspaceMember(workspaceId, userId, "MEMBER");

            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/billing/workspaces/{workspaceId}/payments",
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders(userJwt)),
                    String.class,
                    workspaceId
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/billing/workspaces/{workspaceId}/usage/aggregations")
    class GetUsageAggregations {

        @Test
        @DisplayName("Should return usage aggregations for owner")
        void shouldReturnUsageAggregations() {
            Plan plan = testDataBuilder.createProPlan();
            Subscription subscription = testDataBuilder.createActiveSubscription(workspaceId, plan);
            testDataBuilder.createUsageLog(subscription, userId, "gpt-4", 100, 200, new BigDecimal("0.05"));
            wireMockStubs.stubWorkspaceServiceGetWorkspaceMember(workspaceId, userId, "OWNER");

            ResponseEntity<UsageAggregationResponse> response = restTemplate.exchange(
                    "/api/v1/billing/workspaces/{workspaceId}/usage/aggregations?granularity=DAILY",
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders(userJwt)),
                    UsageAggregationResponse.class,
                    workspaceId
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("Should return 403 for non-owner")
        void shouldReturn403ForNonOwner() {
            wireMockStubs.stubWorkspaceServiceGetWorkspaceMember(workspaceId, userId, "MEMBER");

            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/billing/workspaces/{workspaceId}/usage/aggregations?granularity=DAILY",
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders(userJwt)),
                    String.class,
                    workspaceId
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }
}
