package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.TestFixtures;
import com.baskaaleksander.nuvine.application.dto.CheckLimitRequest;
import com.baskaaleksander.nuvine.application.dto.CheckLimitResult;
import com.baskaaleksander.nuvine.domain.exception.ModelNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.PlanNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.SubscriptionNotFoundException;
import com.baskaaleksander.nuvine.domain.model.*;
import com.baskaaleksander.nuvine.infrastructure.persistence.LlmModelRepository;
import com.baskaaleksander.nuvine.infrastructure.persistence.PlanRepository;
import com.baskaaleksander.nuvine.infrastructure.persistence.SubscriptionRepository;
import com.baskaaleksander.nuvine.infrastructure.persistence.SubscriptionUsageCounterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingInternalServiceTest {

    @Mock
    private LlmModelRepository llmModelRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private SubscriptionUsageCounterRepository usageCounterRepository;

    @Mock
    private ModelPricingService modelPricingService;

    @Mock
    private SubscriptionCacheService subscriptionCacheService;

    @Mock
    private PlanService planService;

    @InjectMocks
    private BillingInternalService billingInternalService;

    private static final ZoneId UTC = ZoneId.of("UTC");

    private Subscription subscription;
    private Plan plan;
    private LlmModel llmModel;
    private SubscriptionUsageCounter usageCounter;
    private CheckLimitRequest request;

    @BeforeEach
    void setUp() {
        subscription = TestFixtures.activeSubscription().build();
        plan = TestFixtures.freePlan()
                .includedCredits(10000L)
                .build();
        llmModel = TestFixtures.llmModel()
                .maxOutputTokens(4096L)
                .build();
        usageCounter = TestFixtures.usageCounter()
                .usedValue(BigDecimal.valueOf(1000))
                .reservedBudget(BigDecimal.valueOf(500))
                .build();
        request = TestFixtures.checkLimitRequest();
    }

    @Test
    void checkAndReserveLimit_sufficientCredits_reservesBudgetAndReturnsApproved() {
        BigDecimal rawCost = new BigDecimal("0.1");
        BigDecimal expectedCredits = BigDecimal.valueOf(100);

        when(llmModelRepository.findActiveModel(eq(request.providerKey()), eq(request.modelKey()), any(Instant.class)))
                .thenReturn(Optional.of(llmModel));
        when(subscriptionCacheService.findByWorkspaceId(request.workspaceId()))
                .thenReturn(Optional.of(subscription));
        when(planService.findById(subscription.getPlanId()))
                .thenReturn(Optional.of(plan));
        when(modelPricingService.calculateCost(request.providerKey(), request.modelKey(), request.inputTokens(), 4096L))
                .thenReturn(rawCost);
        when(usageCounterRepository.findCurrentSubscriptionUsageCounter(
                eq(subscription.getId()), any(LocalDate.class), any(LocalDate.class), eq(UsageMetric.CREDITS)))
                .thenReturn(Optional.of(usageCounter));

        CheckLimitResult result = billingInternalService.checkAndReserveLimit(request);

        assertTrue(result.approved());
        assertEquals(0, expectedCredits.compareTo(result.estimatedCost()));
        assertEquals(usageCounter.getUsedValue(), result.usedCredits());
        assertEquals(BigDecimal.valueOf(10000), result.limitCredits());

        verify(usageCounterRepository).incrementReservedBudget(
                eq(subscription.getId()),
                any(LocalDate.class),
                any(LocalDate.class),
                eq(UsageMetric.CREDITS),
                argThat(bd -> bd.compareTo(expectedCredits) == 0)
        );
    }

    @Test
    void checkAndReserveLimit_insufficientCredits_returnsRejected() {
        usageCounter = TestFixtures.usageCounter()
                .usedValue(BigDecimal.valueOf(9000))
                .reservedBudget(BigDecimal.valueOf(500))
                .build();
        BigDecimal rawCost = new BigDecimal("0.6");
        BigDecimal expectedCredits = BigDecimal.valueOf(600);

        when(llmModelRepository.findActiveModel(eq(request.providerKey()), eq(request.modelKey()), any(Instant.class)))
                .thenReturn(Optional.of(llmModel));
        when(subscriptionCacheService.findByWorkspaceId(request.workspaceId()))
                .thenReturn(Optional.of(subscription));
        when(planService.findById(subscription.getPlanId()))
                .thenReturn(Optional.of(plan));
        when(modelPricingService.calculateCost(request.providerKey(), request.modelKey(), request.inputTokens(), 4096L))
                .thenReturn(rawCost);
        when(usageCounterRepository.findCurrentSubscriptionUsageCounter(
                eq(subscription.getId()), any(LocalDate.class), any(LocalDate.class), eq(UsageMetric.CREDITS)))
                .thenReturn(Optional.of(usageCounter));

        CheckLimitResult result = billingInternalService.checkAndReserveLimit(request);

        assertFalse(result.approved());
        assertEquals(0, expectedCredits.compareTo(result.estimatedCost()));
        assertEquals(usageCounter.getUsedValue(), result.usedCredits());
        assertEquals(usageCounter.getReservedBudget(), result.reservedCredits());
        assertEquals(BigDecimal.valueOf(10000), result.limitCredits());

        verify(usageCounterRepository, never()).incrementReservedBudget(any(), any(), any(), any(), any());
        verify(usageCounterRepository, never()).save(any());
    }

    @Test
    void checkAndReserveLimit_noSubscription_throwsSubscriptionNotFoundException() {
        when(llmModelRepository.findActiveModel(eq(request.providerKey()), eq(request.modelKey()), any(Instant.class)))
                .thenReturn(Optional.of(llmModel));
        when(subscriptionCacheService.findByWorkspaceId(request.workspaceId()))
                .thenReturn(Optional.empty());

        SubscriptionNotFoundException exception = assertThrows(
                SubscriptionNotFoundException.class,
                () -> billingInternalService.checkAndReserveLimit(request)
        );

        assertTrue(exception.getMessage().contains(request.workspaceId().toString()));
        verify(planService, never()).findById(any());
    }

    @Test
    void checkAndReserveLimit_modelNotFound_throwsModelNotFoundException() {
        when(llmModelRepository.findActiveModel(eq(request.providerKey()), eq(request.modelKey()), any(Instant.class)))
                .thenReturn(Optional.empty());

        ModelNotFoundException exception = assertThrows(
                ModelNotFoundException.class,
                () -> billingInternalService.checkAndReserveLimit(request)
        );

        assertTrue(exception.getMessage().contains(request.providerKey()));
        assertTrue(exception.getMessage().contains(request.modelKey()));
        verify(subscriptionCacheService, never()).findByWorkspaceId(any());
    }

    @Test
    void checkAndReserveLimit_planNotFound_throwsPlanNotFoundException() {
        when(llmModelRepository.findActiveModel(eq(request.providerKey()), eq(request.modelKey()), any(Instant.class)))
                .thenReturn(Optional.of(llmModel));
        when(subscriptionCacheService.findByWorkspaceId(request.workspaceId()))
                .thenReturn(Optional.of(subscription));
        when(planService.findById(subscription.getPlanId()))
                .thenReturn(Optional.empty());

        PlanNotFoundException exception = assertThrows(
                PlanNotFoundException.class,
                () -> billingInternalService.checkAndReserveLimit(request)
        );

        assertTrue(exception.getMessage().contains(subscription.getPlanId().toString()));
    }

    @Test
    void checkAndReserveLimit_noExistingCounter_createsNewCounter() {
        BigDecimal rawCost = new BigDecimal("0.1");
        BigDecimal expectedCredits = BigDecimal.valueOf(100);

        when(llmModelRepository.findActiveModel(eq(request.providerKey()), eq(request.modelKey()), any(Instant.class)))
                .thenReturn(Optional.of(llmModel));
        when(subscriptionCacheService.findByWorkspaceId(request.workspaceId()))
                .thenReturn(Optional.of(subscription));
        when(planService.findById(subscription.getPlanId()))
                .thenReturn(Optional.of(plan));
        when(modelPricingService.calculateCost(request.providerKey(), request.modelKey(), request.inputTokens(), 4096L))
                .thenReturn(rawCost);
        when(usageCounterRepository.findCurrentSubscriptionUsageCounter(
                eq(subscription.getId()), any(LocalDate.class), any(LocalDate.class), eq(UsageMetric.CREDITS)))
                .thenReturn(Optional.empty());

        CheckLimitResult result = billingInternalService.checkAndReserveLimit(request);

        assertTrue(result.approved());

        ArgumentCaptor<SubscriptionUsageCounter> counterCaptor = ArgumentCaptor.forClass(SubscriptionUsageCounter.class);
        verify(usageCounterRepository).save(counterCaptor.capture());

        SubscriptionUsageCounter savedCounter = counterCaptor.getValue();
        assertEquals(subscription.getId(), savedCounter.getSubscriptionId());
        assertEquals(BigDecimal.ZERO, savedCounter.getUsedValue());
        assertEquals(0, expectedCredits.compareTo(savedCounter.getReservedBudget()));
        assertEquals(UsageMetric.CREDITS, savedCounter.getMetric());
    }

    @Test
    void checkAndReserveLimit_raceConditionOnCounterCreation_fallsBackToUpdate() {
        BigDecimal rawCost = new BigDecimal("0.1");
        BigDecimal expectedCredits = BigDecimal.valueOf(100);

        when(llmModelRepository.findActiveModel(eq(request.providerKey()), eq(request.modelKey()), any(Instant.class)))
                .thenReturn(Optional.of(llmModel));
        when(subscriptionCacheService.findByWorkspaceId(request.workspaceId()))
                .thenReturn(Optional.of(subscription));
        when(planService.findById(subscription.getPlanId()))
                .thenReturn(Optional.of(plan));
        when(modelPricingService.calculateCost(request.providerKey(), request.modelKey(), request.inputTokens(), 4096L))
                .thenReturn(rawCost);
        when(usageCounterRepository.findCurrentSubscriptionUsageCounter(
                eq(subscription.getId()), any(LocalDate.class), any(LocalDate.class), eq(UsageMetric.CREDITS)))
                .thenReturn(Optional.empty());

        when(usageCounterRepository.save(any(SubscriptionUsageCounter.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key"));

        CheckLimitResult result = billingInternalService.checkAndReserveLimit(request);

        assertTrue(result.approved());

        verify(usageCounterRepository).incrementReservedBudget(
                eq(subscription.getId()),
                any(LocalDate.class),
                any(LocalDate.class),
                eq(UsageMetric.CREDITS),
                argThat(bd -> bd.compareTo(expectedCredits) == 0)
        );
    }

    @Test
    void checkAndReserveLimit_modelWithZeroMaxOutputTokens_usesDefaultMaxTokens() {
        llmModel = TestFixtures.llmModel()
                .maxOutputTokens(0L)
                .build();
        BigDecimal rawCost = new BigDecimal("0.1");

        when(llmModelRepository.findActiveModel(eq(request.providerKey()), eq(request.modelKey()), any(Instant.class)))
                .thenReturn(Optional.of(llmModel));
        when(subscriptionCacheService.findByWorkspaceId(request.workspaceId()))
                .thenReturn(Optional.of(subscription));
        when(planService.findById(subscription.getPlanId()))
                .thenReturn(Optional.of(plan));
        when(modelPricingService.calculateCost(eq(request.providerKey()), eq(request.modelKey()), eq(request.inputTokens()), eq(4096L)))
                .thenReturn(rawCost);
        when(usageCounterRepository.findCurrentSubscriptionUsageCounter(
                eq(subscription.getId()), any(LocalDate.class), any(LocalDate.class), eq(UsageMetric.CREDITS)))
                .thenReturn(Optional.of(usageCounter));

        CheckLimitResult result = billingInternalService.checkAndReserveLimit(request);

        assertTrue(result.approved());
        verify(modelPricingService).calculateCost(request.providerKey(), request.modelKey(), request.inputTokens(), 4096L);
    }

    @Test
    void releaseReservation_validWorkspace_decrementsReservedBudget() {
        UUID workspaceId = TestFixtures.DEFAULT_WORKSPACE_ID;
        BigDecimal amount = BigDecimal.valueOf(100);

        when(subscriptionCacheService.findByWorkspaceId(workspaceId))
                .thenReturn(Optional.of(subscription));

        billingInternalService.releaseReservation(workspaceId, amount);

        verify(usageCounterRepository).decrementReservedBudget(
                eq(subscription.getId()),
                any(LocalDate.class),
                any(LocalDate.class),
                eq(UsageMetric.CREDITS),
                eq(amount)
        );
    }

    @Test
    void releaseReservation_noSubscription_throwsNoSuchElementException() {
        UUID workspaceId = TestFixtures.DEFAULT_WORKSPACE_ID;
        BigDecimal amount = BigDecimal.valueOf(100);

        when(subscriptionCacheService.findByWorkspaceId(workspaceId))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> billingInternalService.releaseReservation(workspaceId, amount)
        );

        verify(usageCounterRepository, never()).decrementReservedBudget(any(), any(), any(), any(), any());
    }
}
