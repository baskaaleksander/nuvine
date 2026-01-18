package com.baskaaleksander.nuvine.integration.controller;

import com.baskaaleksander.nuvine.application.dto.CheckLimitRequest;
import com.baskaaleksander.nuvine.application.dto.CheckLimitResult;
import com.baskaaleksander.nuvine.application.dto.ReleaseReservationRequest;
import com.baskaaleksander.nuvine.domain.model.*;
import com.baskaaleksander.nuvine.infrastructure.persistence.SubscriptionUsageCounterRepository;
import com.baskaaleksander.nuvine.integration.base.BaseControllerIntegrationTest;
import com.baskaaleksander.nuvine.integration.support.JwtTestUtils;
import com.baskaaleksander.nuvine.integration.support.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for BillingInternalController.
 * Tests internal billing endpoints for limit checking and reservation management.
 */
class BillingInternalControllerIT extends BaseControllerIntegrationTest {

    @Autowired
    private TestDataBuilder testDataBuilder;

    @Autowired
    private SubscriptionUsageCounterRepository usageCounterRepository;

    private JwtTestUtils jwtTestUtils;

    private UUID workspaceId;
    private String internalServiceJwt;
    private String userJwt;

    @BeforeEach
    void setUp() {
        testDataBuilder.cleanUp();
        jwtTestUtils = new JwtTestUtils("http://localhost:" + wireMockServer.port() + "/realms/nuvine");

        workspaceId = UUID.randomUUID();
        UUID internalServiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        internalServiceJwt = jwtTestUtils.generateJwt(internalServiceId, "internal@system.local", List.of("ROLE_INTERNAL_SERVICE"));
        userJwt = jwtTestUtils.generateJwt(userId, "user@example.com", List.of("ROLE_USER"));
    }

    @Nested
    @DisplayName("POST /api/v1/internal/billing/check-limit")
    class CheckLimit {

        @Test
        @DisplayName("Should approve when within budget")
        void shouldApproveWhenWithinBudget() {
            Plan plan = testDataBuilder.createProPlan();
            Subscription subscription = testDataBuilder.createActiveSubscription(workspaceId, plan);
            LlmProvider provider = testDataBuilder.createLlmProvider("openai", "OpenAI");
            testDataBuilder.createLlmModel(provider, "gpt-4", "GPT-4");

            LocalDate periodStart = subscription.getCurrentPeriodStart()
                    .atZone(ZoneId.of("CET"))
                    .toLocalDate();
            LocalDate periodEnd = subscription.getCurrentPeriodEnd()
                    .atZone(ZoneId.of("CET"))
                    .toLocalDate();
            testDataBuilder.createUsageCounter(subscription, periodStart, periodEnd,
                    new BigDecimal("100"), BigDecimal.ZERO);

            CheckLimitRequest request = new CheckLimitRequest(
                    workspaceId,
                    "gpt-4",
                    "openai",
                    1000L
            );

            ResponseEntity<CheckLimitResult> response = restTemplate.exchange(
                    "/api/v1/internal/billing/check-limit",
                    HttpMethod.POST,
                    new HttpEntity<>(request, authHeaders(internalServiceJwt)),
                    CheckLimitResult.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().approved()).isTrue();
        }

        @Test
        @DisplayName("Should reject when exceeds budget with BLOCK behaviour")
        void shouldRejectWhenExceedsBudgetWithBlock() {
            Plan plan = testDataBuilder.createPlan("TEST_BLOCK", "Test Block Plan",
                    BillingPeriod.MONTHLY, 100L, HardLimitBehaviour.BLOCK);
            Subscription subscription = testDataBuilder.createActiveSubscription(workspaceId, plan);
            LlmProvider provider = testDataBuilder.createLlmProvider("openai", "OpenAI");
            testDataBuilder.createLlmModel(provider, "gpt-4", "GPT-4");

            LocalDate periodStart = subscription.getCurrentPeriodStart()
                    .atZone(ZoneId.of("CET"))
                    .toLocalDate();
            LocalDate periodEnd = subscription.getCurrentPeriodEnd()
                    .atZone(ZoneId.of("CET"))
                    .toLocalDate();
            testDataBuilder.createUsageCounter(subscription, periodStart, periodEnd,
                    new BigDecimal("95"), BigDecimal.ZERO);

            CheckLimitRequest request = new CheckLimitRequest(
                    workspaceId,
                    "gpt-4",
                    "openai",
                    8000000000L
            );

            ResponseEntity<CheckLimitResult> response = restTemplate.exchange(
                    "/api/v1/internal/billing/check-limit",
                    HttpMethod.POST,
                    new HttpEntity<>(request, authHeaders(internalServiceJwt)),
                    CheckLimitResult.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().approved()).isFalse();
        }

        @Test
        @DisplayName("Should create counter if not exists")
        void shouldCreateCounterIfNotExists() {
            Plan plan = testDataBuilder.createProPlan();
            testDataBuilder.createActiveSubscription(workspaceId, plan);
            LlmProvider provider = testDataBuilder.createLlmProvider("openai", "OpenAI");
            testDataBuilder.createLlmModel(provider, "gpt-4", "GPT-4");

            CheckLimitRequest request = new CheckLimitRequest(
                    workspaceId,
                    "gpt-4",
                    "openai",
                    1000L
            );

            ResponseEntity<CheckLimitResult> response = restTemplate.exchange(
                    "/api/v1/internal/billing/check-limit",
                    HttpMethod.POST,
                    new HttpEntity<>(request, authHeaders(internalServiceJwt)),
                    CheckLimitResult.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().approved()).isTrue();
        }

        @Test
        @DisplayName("Should return 403 for non-internal service")
        void shouldReturn403ForNonInternalService() {
            CheckLimitRequest request = new CheckLimitRequest(
                    workspaceId,
                    "gpt-4",
                    "openai",
                    1000L
            );

            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/internal/billing/check-limit",
                    HttpMethod.POST,
                    new HttpEntity<>(request, authHeaders(userJwt)),
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("Should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticated() {
            CheckLimitRequest request = new CheckLimitRequest(
                    workspaceId,
                    "gpt-4",
                    "openai",
                    1000L
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/internal/billing/check-limit",
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/internal/billing/release-reservation")
    class ReleaseReservation {

        @Test
        @DisplayName("Should decrement reserved budget")
        void shouldDecrementReservedBudget() {
            Plan plan = testDataBuilder.createProPlan();
            Subscription subscription = testDataBuilder.createActiveSubscription(workspaceId, plan);

            LocalDate periodStart = subscription.getCurrentPeriodStart()
                    .atZone(ZoneId.of("CET"))
                    .toLocalDate();
            LocalDate periodEnd = subscription.getCurrentPeriodEnd()
                    .atZone(ZoneId.of("CET"))
                    .toLocalDate();
            testDataBuilder.createUsageCounter(
                    subscription, periodStart, periodEnd,
                    new BigDecimal("100"), new BigDecimal("50"));

            ReleaseReservationRequest request = new ReleaseReservationRequest(
                    workspaceId,
                    new BigDecimal("20")
            );

            ResponseEntity<Void> response = restTemplate.exchange(
                    "/api/v1/internal/billing/release-reservation",
                    HttpMethod.POST,
                    new HttpEntity<>(request, authHeaders(internalServiceJwt)),
                    Void.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            Optional<SubscriptionUsageCounter> updatedCounter = usageCounterRepository
                    .findCurrentSubscriptionUsageCounter(
                            subscription.getId(), periodStart, periodEnd, UsageMetric.CREDITS);
            assertThat(updatedCounter).isPresent();
            assertThat(updatedCounter.get().getReservedBudget()).isEqualByComparingTo(new BigDecimal("30"));
        }

        @Test
        @DisplayName("Should return 403 for non-internal service")
        void shouldReturn403ForNonInternalService() {
            ReleaseReservationRequest request = new ReleaseReservationRequest(
                    workspaceId,
                    new BigDecimal("20")
            );

            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/internal/billing/release-reservation",
                    HttpMethod.POST,
                    new HttpEntity<>(request, authHeaders(userJwt)),
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }
}
