package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.TestFixtures;
import com.baskaaleksander.nuvine.application.dto.*;
import com.baskaaleksander.nuvine.application.mapper.BillingMapper;
import com.baskaaleksander.nuvine.domain.exception.PlanNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.SubscriptionNotFoundException;
import com.baskaaleksander.nuvine.domain.model.*;
import com.baskaaleksander.nuvine.infrastructure.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BillingService")
class BillingServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private SubscriptionUsageCounterRepository usageCounterRepository;

    @Mock
    private UsageLogRepository usageLogRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BillingMapper billingMapper;

    @InjectMocks
    private BillingService billingService;

    private Subscription subscription;
    private Plan plan;
    private SubscriptionUsageCounter counter;

    @BeforeEach
    void setUp() {
        subscription = TestFixtures.activeSubscription().build();
        plan = TestFixtures.freePlan().build();
        counter = TestFixtures.usageCounter()
                .subscriptionId(subscription.getId())
                .build();
    }

    @Nested
    @DisplayName("getSubscriptionStatus")
    class GetSubscriptionStatus {

        @Test
        @DisplayName("should return subscription status with usage counter")
        void getSubscriptionStatus_validWorkspace_returnsStatus() {
            UUID workspaceId = TestFixtures.DEFAULT_WORKSPACE_ID;
            SubscriptionStatusResponse expectedResponse = createSubscriptionStatusResponse();

            when(subscriptionRepository.findByWorkspaceId(workspaceId))
                    .thenReturn(Optional.of(subscription));
            when(planRepository.findById(subscription.getPlanId()))
                    .thenReturn(Optional.of(plan));
            when(usageCounterRepository.findCurrentSubscriptionUsageCounter(
                    eq(subscription.getId()), any(LocalDate.class), any(LocalDate.class), eq(UsageMetric.CREDITS)))
                    .thenReturn(Optional.of(counter));
            when(billingMapper.toSubscriptionStatusResponse(subscription, plan, counter))
                    .thenReturn(expectedResponse);

            SubscriptionStatusResponse result = billingService.getSubscriptionStatus(workspaceId);

            assertThat(result).isEqualTo(expectedResponse);
            verify(subscriptionRepository).findByWorkspaceId(workspaceId);
            verify(planRepository).findById(subscription.getPlanId());
            verify(usageCounterRepository).findCurrentSubscriptionUsageCounter(
                    eq(subscription.getId()), any(LocalDate.class), any(LocalDate.class), eq(UsageMetric.CREDITS));
        }

        @Test
        @DisplayName("should create empty counter when no usage counter exists")
        void getSubscriptionStatus_noUsageCounter_createsEmptyCounter() {
            UUID workspaceId = TestFixtures.DEFAULT_WORKSPACE_ID;

            when(subscriptionRepository.findByWorkspaceId(workspaceId))
                    .thenReturn(Optional.of(subscription));
            when(planRepository.findById(subscription.getPlanId()))
                    .thenReturn(Optional.of(plan));
            when(usageCounterRepository.findCurrentSubscriptionUsageCounter(
                    eq(subscription.getId()), any(LocalDate.class), any(LocalDate.class), eq(UsageMetric.CREDITS)))
                    .thenReturn(Optional.empty());
            when(billingMapper.toSubscriptionStatusResponse(eq(subscription), eq(plan), any(SubscriptionUsageCounter.class)))
                    .thenAnswer(invocation -> {
                        SubscriptionUsageCounter emptyCounter = invocation.getArgument(2);
                        assertThat(emptyCounter.getUsedValue()).isEqualByComparingTo(BigDecimal.ZERO);
                        assertThat(emptyCounter.getReservedBudget()).isEqualByComparingTo(BigDecimal.ZERO);
                        return createSubscriptionStatusResponse();
                    });

            billingService.getSubscriptionStatus(workspaceId);

            verify(billingMapper).toSubscriptionStatusResponse(eq(subscription), eq(plan), any(SubscriptionUsageCounter.class));
        }

        @Test
        @DisplayName("should throw SubscriptionNotFoundException when subscription not found")
        void getSubscriptionStatus_noSubscription_throwsException() {
            UUID workspaceId = TestFixtures.DEFAULT_WORKSPACE_ID;

            when(subscriptionRepository.findByWorkspaceId(workspaceId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> billingService.getSubscriptionStatus(workspaceId))
                    .isInstanceOf(SubscriptionNotFoundException.class)
                    .hasMessageContaining(workspaceId.toString());
        }

        @Test
        @DisplayName("should throw PlanNotFoundException when plan not found")
        void getSubscriptionStatus_noPlan_throwsException() {
            UUID workspaceId = TestFixtures.DEFAULT_WORKSPACE_ID;

            when(subscriptionRepository.findByWorkspaceId(workspaceId))
                    .thenReturn(Optional.of(subscription));
            when(planRepository.findById(subscription.getPlanId()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> billingService.getSubscriptionStatus(workspaceId))
                    .isInstanceOf(PlanNotFoundException.class)
                    .hasMessageContaining(subscription.getPlanId().toString());
        }
    }

    @Nested
    @DisplayName("getUsageLogs")
    class GetUsageLogs {

        @Test
        @DisplayName("should return paginated usage logs without filters")
        void getUsageLogs_noFilters_returnsLogs() {
            UUID workspaceId = TestFixtures.DEFAULT_WORKSPACE_ID;
            UsageLogFilterRequest filter = UsageLogFilterRequest.builder()
                    .startDate(LocalDate.now().minusDays(7))
                    .endDate(LocalDate.now())
                    .build();

            UsageLog log1 = TestFixtures.usageLog().build();
            UsageLog log2 = TestFixtures.usageLog().id(TestFixtures.randomId()).build();
            Page<UsageLog> page = new PageImpl<>(List.of(log1, log2));

            UsageLogResponse response1 = createUsageLogResponse(log1);
            UsageLogResponse response2 = createUsageLogResponse(log2);

            when(usageLogRepository.findByWorkspaceIdAndDateRange(
                    eq(workspaceId), any(Instant.class), any(Instant.class), any(Pageable.class)))
                    .thenReturn(page);
            when(billingMapper.toUsageLogResponse(log1)).thenReturn(response1);
            when(billingMapper.toUsageLogResponse(log2)).thenReturn(response2);

            PagedResponse<UsageLogResponse> result = billingService.getUsageLogs(workspaceId, filter);

            assertThat(result.content()).hasSize(2);
            assertThat(result.totalElements()).isEqualTo(2);
            verify(usageLogRepository).findByWorkspaceIdAndDateRange(
                    eq(workspaceId), any(Instant.class), any(Instant.class), any(Pageable.class));
        }

        @Test
        @DisplayName("should filter by provider")
        void getUsageLogs_withProviderFilter_filtersCorrectly() {
            UUID workspaceId = TestFixtures.DEFAULT_WORKSPACE_ID;
            UsageLogFilterRequest filter = UsageLogFilterRequest.builder()
                    .startDate(LocalDate.now().minusDays(7))
                    .endDate(LocalDate.now())
                    .provider("openai")
                    .build();

            Page<UsageLog> page = new PageImpl<>(List.of());

            when(usageLogRepository.findByWorkspaceIdAndDateRangeAndProvider(
                    eq(workspaceId), any(Instant.class), any(Instant.class), eq("openai"), any(Pageable.class)))
                    .thenReturn(page);

            billingService.getUsageLogs(workspaceId, filter);

            verify(usageLogRepository).findByWorkspaceIdAndDateRangeAndProvider(
                    eq(workspaceId), any(Instant.class), any(Instant.class), eq("openai"), any(Pageable.class));
        }

        @Test
        @DisplayName("should filter by model")
        void getUsageLogs_withModelFilter_filtersCorrectly() {
            UUID workspaceId = TestFixtures.DEFAULT_WORKSPACE_ID;
            UsageLogFilterRequest filter = UsageLogFilterRequest.builder()
                    .startDate(LocalDate.now().minusDays(7))
                    .endDate(LocalDate.now())
                    .model("gpt-4")
                    .build();

            Page<UsageLog> page = new PageImpl<>(List.of());

            when(usageLogRepository.findByWorkspaceIdAndDateRangeAndModel(
                    eq(workspaceId), any(Instant.class), any(Instant.class), eq("gpt-4"), any(Pageable.class)))
                    .thenReturn(page);

            billingService.getUsageLogs(workspaceId, filter);

            verify(usageLogRepository).findByWorkspaceIdAndDateRangeAndModel(
                    eq(workspaceId), any(Instant.class), any(Instant.class), eq("gpt-4"), any(Pageable.class));
        }

        @Test
        @DisplayName("should filter by both provider and model")
        void getUsageLogs_withProviderAndModelFilter_filtersCorrectly() {
            UUID workspaceId = TestFixtures.DEFAULT_WORKSPACE_ID;
            UsageLogFilterRequest filter = UsageLogFilterRequest.builder()
                    .startDate(LocalDate.now().minusDays(7))
                    .endDate(LocalDate.now())
                    .provider("openai")
                    .model("gpt-4")
                    .build();

            Page<UsageLog> page = new PageImpl<>(List.of());

            when(usageLogRepository.findByWorkspaceIdAndDateRangeAndProviderAndModel(
                    eq(workspaceId), any(Instant.class), any(Instant.class), eq("openai"), eq("gpt-4"), any(Pageable.class)))
                    .thenReturn(page);

            billingService.getUsageLogs(workspaceId, filter);

            verify(usageLogRepository).findByWorkspaceIdAndDateRangeAndProviderAndModel(
                    eq(workspaceId), any(Instant.class), any(Instant.class), eq("openai"), eq("gpt-4"), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("getPayments")
    class GetPayments {

        @Test
        @DisplayName("should return paginated payments without status filter")
        void getPayments_noStatusFilter_returnsPayments() {
            UUID workspaceId = TestFixtures.DEFAULT_WORKSPACE_ID;
            PaymentFilterRequest filter = PaymentFilterRequest.builder()
                    .startDate(LocalDate.now().minusDays(30))
                    .endDate(LocalDate.now())
                    .build();

            Payment payment = TestFixtures.payment().build();
            Page<Payment> page = new PageImpl<>(List.of(payment));
            PaymentResponse paymentResponse = createPaymentResponse(payment);

            when(paymentRepository.findByWorkspaceIdAndDateRange(
                    eq(workspaceId), any(Instant.class), any(Instant.class), any(Pageable.class)))
                    .thenReturn(page);
            when(billingMapper.toPaymentResponse(payment)).thenReturn(paymentResponse);

            PagedResponse<PaymentResponse> result = billingService.getPayments(workspaceId, filter);

            assertThat(result.content()).hasSize(1);
            verify(paymentRepository).findByWorkspaceIdAndDateRange(
                    eq(workspaceId), any(Instant.class), any(Instant.class), any(Pageable.class));
        }

        @Test
        @DisplayName("should filter by payment status")
        void getPayments_withStatusFilter_filtersCorrectly() {
            UUID workspaceId = TestFixtures.DEFAULT_WORKSPACE_ID;
            PaymentFilterRequest filter = PaymentFilterRequest.builder()
                    .startDate(LocalDate.now().minusDays(30))
                    .endDate(LocalDate.now())
                    .status(PaymentStatus.PAID)
                    .build();

            Page<Payment> page = new PageImpl<>(List.of());

            when(paymentRepository.findByWorkspaceIdAndDateRangeAndStatus(
                    eq(workspaceId), any(Instant.class), any(Instant.class), eq(PaymentStatus.PAID), any(Pageable.class)))
                    .thenReturn(page);

            billingService.getPayments(workspaceId, filter);

            verify(paymentRepository).findByWorkspaceIdAndDateRangeAndStatus(
                    eq(workspaceId), any(Instant.class), any(Instant.class), eq(PaymentStatus.PAID), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("getUsageAggregations")
    class GetUsageAggregations {

        @Test
        @DisplayName("should aggregate usage by daily granularity")
        void getUsageAggregations_dailyGranularity_aggregatesCorrectly() {
            UUID workspaceId = TestFixtures.DEFAULT_WORKSPACE_ID;
            LocalDate today = LocalDate.now();
            UsageAggregationRequest request = UsageAggregationRequest.builder()
                    .startDate(today.minusDays(7))
                    .endDate(today)
                    .granularity(AggregationGranularity.DAILY)
                    .build();

            Instant todayInstant = today.atStartOfDay(ZoneOffset.UTC).toInstant();
            UsageLog log1 = TestFixtures.usageLog()
                    .occurredAt(todayInstant)
                    .tokensIn(1000L)
                    .tokensOut(500L)
                    .costCredits(BigDecimal.valueOf(45))
                    .build();
            UsageLog log2 = TestFixtures.usageLog()
                    .id(TestFixtures.randomId())
                    .occurredAt(todayInstant.minusSeconds(3600))
                    .tokensIn(2000L)
                    .tokensOut(1000L)
                    .costCredits(BigDecimal.valueOf(90))
                    .build();

            when(usageLogRepository.findByWorkspaceIdAndDateRangeList(
                    eq(workspaceId), any(Instant.class), any(Instant.class)))
                    .thenReturn(List.of(log1, log2));

            UsageAggregationResponse result = billingService.getUsageAggregations(workspaceId, request);

            assertThat(result.totalRequests()).isEqualTo(2);
            assertThat(result.totalTokensIn()).isEqualTo(3000L);
            assertThat(result.totalTokensOut()).isEqualTo(1500L);
            assertThat(result.totalCostCredits()).isEqualByComparingTo(BigDecimal.valueOf(135));
        }

        @Test
        @DisplayName("should aggregate usage by model")
        void getUsageAggregations_aggregatesByModel() {
            UUID workspaceId = TestFixtures.DEFAULT_WORKSPACE_ID;
            UsageAggregationRequest request = UsageAggregationRequest.builder()
                    .startDate(LocalDate.now().minusDays(7))
                    .endDate(LocalDate.now())
                    .granularity(AggregationGranularity.DAILY)
                    .build();

            UsageLog gpt4Log = TestFixtures.usageLog()
                    .provider("openai")
                    .model("gpt-4")
                    .tokensIn(1000L)
                    .tokensOut(500L)
                    .build();
            UsageLog gpt35Log = TestFixtures.usageLog()
                    .id(TestFixtures.randomId())
                    .provider("openai")
                    .model("gpt-3.5-turbo")
                    .tokensIn(2000L)
                    .tokensOut(1000L)
                    .build();

            when(usageLogRepository.findByWorkspaceIdAndDateRangeList(
                    eq(workspaceId), any(Instant.class), any(Instant.class)))
                    .thenReturn(List.of(gpt4Log, gpt35Log));

            UsageAggregationResponse result = billingService.getUsageAggregations(workspaceId, request);

            assertThat(result.modelBasedUsage()).hasSize(2);
        }

        @Test
        @DisplayName("should return empty aggregations when no logs exist")
        void getUsageAggregations_noLogs_returnsEmptyAggregations() {
            UUID workspaceId = TestFixtures.DEFAULT_WORKSPACE_ID;
            UsageAggregationRequest request = UsageAggregationRequest.builder()
                    .startDate(LocalDate.now().minusDays(7))
                    .endDate(LocalDate.now())
                    .granularity(AggregationGranularity.DAILY)
                    .build();

            when(usageLogRepository.findByWorkspaceIdAndDateRangeList(
                    eq(workspaceId), any(Instant.class), any(Instant.class)))
                    .thenReturn(List.of());

            UsageAggregationResponse result = billingService.getUsageAggregations(workspaceId, request);

            assertThat(result.totalRequests()).isZero();
            assertThat(result.totalTokensIn()).isZero();
            assertThat(result.totalTokensOut()).isZero();
            assertThat(result.totalCostCredits()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.timeBasedUsage()).isEmpty();
            assertThat(result.modelBasedUsage()).isEmpty();
        }

        @Test
        @DisplayName("should aggregate by weekly granularity")
        void getUsageAggregations_weeklyGranularity_aggregatesCorrectly() {
            UUID workspaceId = TestFixtures.DEFAULT_WORKSPACE_ID;
            LocalDate monday = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
            UsageAggregationRequest request = UsageAggregationRequest.builder()
                    .startDate(monday.minusWeeks(1))
                    .endDate(monday.plusDays(6))
                    .granularity(AggregationGranularity.WEEKLY)
                    .build();

            Instant mondayInstant = monday.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant tuesdayInstant = monday.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

            UsageLog log1 = TestFixtures.usageLog()
                    .occurredAt(mondayInstant)
                    .tokensIn(1000L)
                    .build();
            UsageLog log2 = TestFixtures.usageLog()
                    .id(TestFixtures.randomId())
                    .occurredAt(tuesdayInstant)
                    .tokensIn(2000L)
                    .build();

            when(usageLogRepository.findByWorkspaceIdAndDateRangeList(
                    eq(workspaceId), any(Instant.class), any(Instant.class)))
                    .thenReturn(List.of(log1, log2));

            UsageAggregationResponse result = billingService.getUsageAggregations(workspaceId, request);

            assertThat(result.timeBasedUsage()).hasSize(1);
            assertThat(result.timeBasedUsage().get(0).tokensIn()).isEqualTo(3000L);
        }
    }

    private SubscriptionStatusResponse createSubscriptionStatusResponse() {
        return new SubscriptionStatusResponse(
                subscription.getId(),
                subscription.getStatus(),
                subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd(),
                subscription.getCancelAtPeriodEnd(),
                new PlanSummaryResponse(
                        plan.getId(),
                        plan.getCode(),
                        plan.getName(),
                        plan.getBillingPeriod(),
                        plan.getIncludedCredits()
                ),
                new UsageSummaryResponse(
                        counter.getUsedValue(),
                        counter.getReservedBudget(),
                        plan.getIncludedCredits(),
                        BigDecimal.valueOf(plan.getIncludedCredits()).subtract(counter.getUsedValue()),
                        counter.getPeriodStart(),
                        counter.getPeriodEnd()
                )
        );
    }

    private UsageLogResponse createUsageLogResponse(UsageLog log) {
        return new UsageLogResponse(
                log.getId(),
                log.getUserId(),
                log.getConversationId(),
                log.getMessageId(),
                log.getProvider(),
                log.getModel(),
                log.getTokensIn(),
                log.getTokensOut(),
                log.getCostCredits(),
                log.getOccurredAt()
        );
    }

    private PaymentResponse createPaymentResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getAmountDue(),
                payment.getAmountPaid(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getBillingPeriodStart(),
                payment.getBillingPeriodEnd(),
                payment.getInvoicePdfUrl(),
                payment.getDescription(),
                payment.getCreatedAt()
        );
    }
}
