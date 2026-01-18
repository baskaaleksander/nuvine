package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.CheckLimitRequest;
import com.baskaaleksander.nuvine.application.dto.CheckLimitResult;
import com.baskaaleksander.nuvine.domain.exception.ModelNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.PlanNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.SubscriptionNotFoundException;
import com.baskaaleksander.nuvine.domain.model.*;
import com.baskaaleksander.nuvine.infrastructure.persistence.LlmModelRepository;
import com.baskaaleksander.nuvine.infrastructure.persistence.SubscriptionUsageCounterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class BillingInternalService {
    private final LlmModelRepository llmModelRepository;
    private final SubscriptionCacheService subscriptionCacheService;
    private final PlanService planService;
    private final SubscriptionUsageCounterRepository usageCounterRepository;
    private final ModelPricingService modelPricingService;

    private static final ZoneId UTC = ZoneId.of("CET");
    private static final long DEFAULT_MAX_OUTPUT_TOKENS = 4096L;

    @Transactional
    public CheckLimitResult checkAndReserveLimit(CheckLimitRequest request) {
        log.info("CHECK_LIMIT START workspace={} model={}:{}",
                request.workspaceId(), request.providerKey(), request.modelKey());

        log.info("Searching in DB: providerKey='{}', modelKey='{}'",
                request.providerKey(), request.modelKey());

        LlmModel model = llmModelRepository
                .findActiveModel(request.providerKey(), request.modelKey(), Instant.now())
                .orElseThrow(() -> new ModelNotFoundException(
                        "Model not found: " + request.providerKey() + ":" + request.modelKey()
                ));

        Subscription subscription = subscriptionCacheService
                .findByWorkspaceId(request.workspaceId())
                .orElseThrow(() -> new SubscriptionNotFoundException(
                        "Subscription not found for workspace: " + request.workspaceId()
                ));

        Plan plan = planService
                .findById(subscription.getPlanId())
                .orElseThrow(() -> new PlanNotFoundException(
                        "Plan not found: " + subscription.getPlanId()
                ));

        long maxOutputTokens = model.getMaxOutputTokens() > 0
                ? model.getMaxOutputTokens()
                : DEFAULT_MAX_OUTPUT_TOKENS;

        BigDecimal estimatedCost = modelPricingService.calculateCost(
                request.providerKey(),
                request.modelKey(),
                request.inputTokens(),
                maxOutputTokens
        ).multiply(BigDecimal.valueOf(1000));

        log.info("Estimated cost: {} credits (input={} output={})",
                estimatedCost, request.inputTokens(), maxOutputTokens);

        LocalDate periodStart = subscription.getCurrentPeriodStart()
                .atZone(UTC)
                .toLocalDate();
        LocalDate periodEnd = subscription.getCurrentPeriodEnd()
                .atZone(UTC)
                .toLocalDate();

        SubscriptionUsageCounter counter = usageCounterRepository
                .findCurrentSubscriptionUsageCounter(
                        subscription.getId(),
                        periodStart,
                        periodEnd,
                        UsageMetric.CREDITS
                )
                .orElse(null);

        BigDecimal includedCredits = BigDecimal.valueOf(plan.getIncludedCredits());
        BigDecimal usedValue = counter != null ? counter.getUsedValue() : BigDecimal.ZERO;
        BigDecimal reservedBudget = counter != null ? counter.getReservedBudget() : BigDecimal.ZERO;

        BigDecimal totalPending = usedValue
                .add(reservedBudget)
                .add(estimatedCost);

        boolean withinLimit = totalPending.compareTo(includedCredits) <= 0;

        if (!withinLimit) {
            log.warn("CHECK_LIMIT REJECT workspace={} used={} reserved={} estimated={} limit={}",
                    request.workspaceId(), usedValue, reservedBudget, estimatedCost, includedCredits);

            return CheckLimitResult.rejected(
                    usedValue,
                    reservedBudget,
                    estimatedCost,
                    includedCredits
            );
        }

        // 6. Atomic reserve budget
        if (counter == null) {
            counter = SubscriptionUsageCounter.builder()
                    .subscriptionId(subscription.getId())
                    .periodStart(periodStart)
                    .periodEnd(periodEnd)
                    .usedValue(BigDecimal.ZERO)
                    .reservedBudget(estimatedCost)
                    .metric(UsageMetric.CREDITS)
                    .build();

            try {
                usageCounterRepository.save(counter);
            } catch (DataIntegrityViolationException e) {
                // Race condition: retry with UPDATE
                usageCounterRepository.incrementReservedBudget(
                        subscription.getId(),
                        periodStart,
                        periodEnd,
                        UsageMetric.CREDITS,
                        estimatedCost
                );
            }
        } else {
            usageCounterRepository.incrementReservedBudget(
                    subscription.getId(),
                    periodStart,
                    periodEnd,
                    UsageMetric.CREDITS,
                    estimatedCost
            );
        }

        log.info("CHECK_LIMIT APPROVED workspace={} reserved={} remaining={}",
                request.workspaceId(), estimatedCost, includedCredits.subtract(totalPending));

        return CheckLimitResult.approved(
                estimatedCost,
                usedValue,
                reservedBudget.add(estimatedCost),
                includedCredits
        );
    }

    @Transactional
    public void releaseReservation(UUID workspaceId, BigDecimal amount) {
        Subscription subscription = subscriptionCacheService
                .findByWorkspaceId(workspaceId)
                .orElseThrow();

        LocalDate periodStart = subscription.getCurrentPeriodStart()
                .atZone(UTC)
                .toLocalDate();
        LocalDate periodEnd = subscription.getCurrentPeriodEnd()
                .atZone(UTC)
                .toLocalDate();

        usageCounterRepository.decrementReservedBudget(
                subscription.getId(),
                periodStart,
                periodEnd,
                UsageMetric.CREDITS,
                amount
        );

        log.debug("Released reservation: workspace={} amount={}", workspaceId, amount);
    }
}