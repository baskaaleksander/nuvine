package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.TestFixtures;
import com.baskaaleksander.nuvine.domain.exception.SubscriptionNotFoundException;
import com.baskaaleksander.nuvine.domain.model.Subscription;
import com.baskaaleksander.nuvine.domain.model.SubscriptionUsageCounter;
import com.baskaaleksander.nuvine.domain.model.UsageLog;
import com.baskaaleksander.nuvine.domain.model.UsageMetric;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.LogTokenUsageEvent;
import com.baskaaleksander.nuvine.infrastructure.persistence.SubscriptionRepository;
import com.baskaaleksander.nuvine.infrastructure.persistence.SubscriptionUsageCounterRepository;
import com.baskaaleksander.nuvine.infrastructure.persistence.UsageLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsageServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UsageLogRepository usageLogRepository;

    @Mock
    private SubscriptionUsageCounterRepository subscriptionUsageCounterRepository;

    @Mock
    private ModelPricingService modelPricingService;

    @InjectMocks
    private UsageService usageService;

    private Subscription subscription;
    private LogTokenUsageEvent event;

    @BeforeEach
    void setUp() {
        subscription = TestFixtures.activeSubscription().build();
        event = TestFixtures.logTokenUsageEvent();
    }

    @Test
    void logTokenUsage_validEvent_savesUsageLogAndUpdatesCounter() {
        BigDecimal costUsd = BigDecimal.valueOf(0.045);
        BigDecimal expectedCostCredits = costUsd.multiply(BigDecimal.valueOf(1000));

        when(subscriptionRepository.findByWorkspaceId(UUID.fromString(event.workspaceId())))
                .thenReturn(Optional.of(subscription));
        when(modelPricingService.calculateCost(event.provider(), event.model(), event.tokensIn(), event.tokensOut()))
                .thenReturn(costUsd);
        when(subscriptionUsageCounterRepository.incrementUsage(
                eq(subscription.getId()), any(LocalDate.class), any(LocalDate.class),
                eq(UsageMetric.CREDITS), eq(expectedCostCredits)))
                .thenReturn(1);

        usageService.logTokenUsage(event);

        ArgumentCaptor<UsageLog> logCaptor = ArgumentCaptor.forClass(UsageLog.class);
        verify(usageLogRepository).save(logCaptor.capture());

        UsageLog savedLog = logCaptor.getValue();
        assertEquals(UUID.fromString(event.workspaceId()), savedLog.getWorkspaceId());
        assertEquals(UUID.fromString(event.userId()), savedLog.getUserId());
        assertEquals(UUID.fromString(event.conversationId()), savedLog.getConversationId());
        assertEquals(UUID.fromString(event.messageId()), savedLog.getMessageId());
        assertEquals(subscription.getId(), savedLog.getSubscriptionId());
        assertEquals(event.model(), savedLog.getModel());
        assertEquals(event.provider(), savedLog.getProvider());
        assertEquals(event.sourceService(), savedLog.getSourceService());
        assertEquals(event.tokensIn(), savedLog.getTokensIn());
        assertEquals(event.tokensOut(), savedLog.getTokensOut());
        assertEquals(0, expectedCostCredits.compareTo(savedLog.getCostCredits()));
        assertEquals(event.occurredAt(), savedLog.getOccurredAt());

        verify(subscriptionUsageCounterRepository).incrementUsage(
                eq(subscription.getId()),
                any(LocalDate.class),
                any(LocalDate.class),
                eq(UsageMetric.CREDITS),
                eq(expectedCostCredits)
        );
    }

    @Test
    void logTokenUsage_calculatesCorrectCostCredits() {
        BigDecimal costUsd = BigDecimal.valueOf(0.1);
        BigDecimal expectedCostCredits = BigDecimal.valueOf(100);

        when(subscriptionRepository.findByWorkspaceId(UUID.fromString(event.workspaceId())))
                .thenReturn(Optional.of(subscription));
        when(modelPricingService.calculateCost(event.provider(), event.model(), event.tokensIn(), event.tokensOut()))
                .thenReturn(costUsd);
        when(subscriptionUsageCounterRepository.incrementUsage(
                eq(subscription.getId()), any(LocalDate.class), any(LocalDate.class),
                eq(UsageMetric.CREDITS), any(BigDecimal.class)))
                .thenReturn(1);

        usageService.logTokenUsage(event);

        ArgumentCaptor<UsageLog> logCaptor = ArgumentCaptor.forClass(UsageLog.class);
        verify(usageLogRepository).save(logCaptor.capture());

        UsageLog savedLog = logCaptor.getValue();
        assertEquals(0, expectedCostCredits.compareTo(savedLog.getCostCredits()));
    }

    @Test
    void logTokenUsage_noExistingCounter_createsNewCounter() {
        BigDecimal costUsd = BigDecimal.valueOf(0.045);
        BigDecimal expectedCostCredits = costUsd.multiply(BigDecimal.valueOf(1000));

        when(subscriptionRepository.findByWorkspaceId(UUID.fromString(event.workspaceId())))
                .thenReturn(Optional.of(subscription));
        when(modelPricingService.calculateCost(event.provider(), event.model(), event.tokensIn(), event.tokensOut()))
                .thenReturn(costUsd);
        when(subscriptionUsageCounterRepository.incrementUsage(
                eq(subscription.getId()), any(LocalDate.class), any(LocalDate.class),
                eq(UsageMetric.CREDITS), eq(expectedCostCredits)))
                .thenReturn(0);

        usageService.logTokenUsage(event);

        ArgumentCaptor<SubscriptionUsageCounter> counterCaptor = ArgumentCaptor.forClass(SubscriptionUsageCounter.class);
        verify(subscriptionUsageCounterRepository).save(counterCaptor.capture());

        SubscriptionUsageCounter savedCounter = counterCaptor.getValue();
        assertEquals(subscription.getId(), savedCounter.getSubscriptionId());
        assertEquals(UsageMetric.CREDITS, savedCounter.getMetric());
        assertEquals(0, expectedCostCredits.compareTo(savedCounter.getUsedValue()));
    }

    @Test
    void logTokenUsage_raceConditionOnCounterCreation_retriesIncrement() {
        BigDecimal costUsd = BigDecimal.valueOf(0.045);
        BigDecimal expectedCostCredits = costUsd.multiply(BigDecimal.valueOf(1000));

        when(subscriptionRepository.findByWorkspaceId(UUID.fromString(event.workspaceId())))
                .thenReturn(Optional.of(subscription));
        when(modelPricingService.calculateCost(event.provider(), event.model(), event.tokensIn(), event.tokensOut()))
                .thenReturn(costUsd);
        when(subscriptionUsageCounterRepository.incrementUsage(
                eq(subscription.getId()), any(LocalDate.class), any(LocalDate.class),
                eq(UsageMetric.CREDITS), eq(expectedCostCredits)))
                .thenReturn(0)
                .thenReturn(1);

        when(subscriptionUsageCounterRepository.save(any(SubscriptionUsageCounter.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key"));

        usageService.logTokenUsage(event);

        verify(subscriptionUsageCounterRepository, times(2)).incrementUsage(
                eq(subscription.getId()),
                any(LocalDate.class),
                any(LocalDate.class),
                eq(UsageMetric.CREDITS),
                eq(expectedCostCredits)
        );
    }

    @Test
    void logTokenUsage_noSubscription_throwsSubscriptionNotFoundException() {
        when(subscriptionRepository.findByWorkspaceId(UUID.fromString(event.workspaceId())))
                .thenReturn(Optional.empty());

        SubscriptionNotFoundException exception = assertThrows(
                SubscriptionNotFoundException.class,
                () -> usageService.logTokenUsage(event)
        );

        assertTrue(exception.getMessage().contains(event.workspaceId()));
        verify(usageLogRepository, never()).save(any());
        verify(subscriptionUsageCounterRepository, never()).incrementUsage(any(), any(), any(), any(), any());
    }
}
