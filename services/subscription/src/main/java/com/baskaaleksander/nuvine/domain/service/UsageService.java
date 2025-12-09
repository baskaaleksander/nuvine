package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.domain.exception.SubscriptionNotFoundException;
import com.baskaaleksander.nuvine.domain.model.Subscription;
import com.baskaaleksander.nuvine.domain.model.SubscriptionUsageCounter;
import com.baskaaleksander.nuvine.domain.model.UsageLog;
import com.baskaaleksander.nuvine.domain.model.UsageMetric;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.LogTokenUsageEvent;
import com.baskaaleksander.nuvine.infrastructure.persistence.SubscriptionRepository;
import com.baskaaleksander.nuvine.infrastructure.persistence.SubscriptionUsageCounterRepository;
import com.baskaaleksander.nuvine.infrastructure.persistence.UsageLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsageService {

    private final SubscriptionRepository subscriptionRepository;
    private final UsageLogRepository usageLogRepository;
    private final SubscriptionUsageCounterRepository subscriptionUsageCounterRepository;
    private final ModelPricingService modelPricingService;

    private static final BigDecimal CREDITS_TO_DOLLARS_EXCHANGE_RATE = BigDecimal.valueOf(1000);
    private static final ZoneId UTC = ZoneId.of("CET");

    @Transactional
    public void logTokenUsage(LogTokenUsageEvent event) {
        log.info("LOG_TOKEN_USAGE START workspaceId={}", event.workspaceId());

        UUID workspaceId = UUID.fromString(event.workspaceId());
        UUID userId = UUID.fromString(event.userId());
        UUID conversationId = UUID.fromString(event.conversationId());
        UUID messageId = UUID.fromString(event.messageId());

        Subscription subscription = subscriptionRepository
                .findByWorkspaceId(workspaceId)
                .orElseThrow(() -> {
                    log.warn("LOG_TOKEN_USAGE END reason=subscription_not_found workspaceId={}", workspaceId);
                    return new SubscriptionNotFoundException("Subscription not found for workspace: " + workspaceId);
                });

        BigDecimal costUsd = modelPricingService.calculateCost(
                event.provider(),
                event.model(),
                event.tokensIn(),
                event.tokensOut()
        );
        BigDecimal costCredits = costUsd.multiply(CREDITS_TO_DOLLARS_EXCHANGE_RATE);

        UsageLog usageLog = UsageLog.builder()
                .workspaceId(workspaceId)
                .userId(userId)
                .conversationId(conversationId)
                .messageId(messageId)
                .model(event.model())
                .provider(event.provider())
                .sourceService(event.sourceService())
                .tokensIn(event.tokensIn())
                .tokensOut(event.tokensOut())
                .costCredits(costCredits)
                .occurredAt(event.occurredAt())
                .build();

        usageLogRepository.save(usageLog);

        LocalDate periodStart = subscription.getCurrentPeriodStart()
                .atZone(UTC)
                .toLocalDate();
        LocalDate periodEnd = subscription.getCurrentPeriodEnd()
                .atZone(UTC)
                .toLocalDate();

        int updated = subscriptionUsageCounterRepository.incrementUsage(
                subscription.getId(),
                periodStart,
                periodEnd,
                UsageMetric.CREDITS,
                costCredits
        );

        if (updated == 0) {
            try {
                SubscriptionUsageCounter counter = SubscriptionUsageCounter.builder()
                        .subscriptionId(subscription.getId())
                        .periodStart(periodStart)
                        .periodEnd(periodEnd)
                        .usedValue(costCredits)
                        .metric(UsageMetric.CREDITS)
                        .build();

                subscriptionUsageCounterRepository.save(counter);
                log.info("Created new usage counter for subscription={} period={} to {}",
                        subscription.getId(), periodStart, periodEnd);
            } catch (DataIntegrityViolationException e) {
                log.debug("Counter already exists, retrying increment");
                subscriptionUsageCounterRepository.incrementUsage(
                        subscription.getId(),
                        periodStart,
                        periodEnd,
                        UsageMetric.CREDITS,
                        costCredits
                );
            }
        }

        log.info("LOG_TOKEN_USAGE END reason=success workspaceId={} cost={} credits",
                workspaceId, costCredits);
    }
}