package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.*;
import com.baskaaleksander.nuvine.application.mapper.BillingMapper;
import com.baskaaleksander.nuvine.application.pagination.PaginationUtil;
import com.baskaaleksander.nuvine.domain.exception.SubscriptionNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.PlanNotFoundException;
import com.baskaaleksander.nuvine.domain.model.*;
import com.baskaaleksander.nuvine.infrastructure.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final SubscriptionUsageCounterRepository usageCounterRepository;
    private final UsageLogRepository usageLogRepository;
    private final PaymentRepository paymentRepository;
    private final BillingMapper billingMapper;

    @Transactional(readOnly = true)
    public SubscriptionStatusResponse getSubscriptionStatus(UUID workspaceId) {
        log.info("GET_SUBSCRIPTION_STATUS START workspaceId={}", workspaceId);

        try {
            Subscription subscription = subscriptionRepository.findByWorkspaceId(workspaceId)
                    .orElseThrow(() -> new SubscriptionNotFoundException(
                            "No subscription found for workspace: " + workspaceId));

            Plan plan = planRepository.findById(subscription.getPlanId())
                    .orElseThrow(() -> new PlanNotFoundException(
                            "Plan not found: " + subscription.getPlanId()));

            LocalDate periodStart = subscription.getCurrentPeriodStart()
                    .atZone(ZoneOffset.UTC).toLocalDate();
            LocalDate periodEnd = subscription.getCurrentPeriodEnd()
                    .atZone(ZoneOffset.UTC).toLocalDate();

            SubscriptionUsageCounter counter = usageCounterRepository
                    .findCurrentSubscriptionUsageCounter(
                            subscription.getId(),
                            periodStart,
                            periodEnd,
                            UsageMetric.CREDITS
                    )
                    .orElseGet(() -> createEmptyCounter(subscription.getId(), periodStart, periodEnd));

            log.info("GET_SUBSCRIPTION_STATUS END workspaceId={}", workspaceId);
            return billingMapper.toSubscriptionStatusResponse(subscription, plan, counter);
        } catch (Exception e) {
            log.error("GET_SUBSCRIPTION_STATUS FAILED workspaceId={} reason={}", workspaceId, e.getMessage());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public PagedResponse<UsageLogResponse> getUsageLogs(
            UUID workspaceId,
            UsageLogFilterRequest filter
    ) {
        log.info("GET_USAGE_LOGS START workspaceId={}", workspaceId);

        try {
            Instant startDate = filter.getStartDate() != null
                    ? filter.getStartDate().atStartOfDay(ZoneOffset.UTC).toInstant()
                    : null;
            Instant endDate = filter.getEndDate() != null
                    ? filter.getEndDate().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
                    : null;

            Page<UsageLog> page = usageLogRepository.findByWorkspaceIdWithFilters(
                    workspaceId,
                    startDate,
                    endDate,
                    filter.getProvider(),
                    filter.getModel(),
                    PaginationUtil.getPageable(filter)
            );

            log.info("GET_USAGE_LOGS END workspaceId={} totalElements={}", workspaceId, page.getTotalElements());
            return new PagedResponse<>(
                    page.getContent().stream()
                            .map(billingMapper::toUsageLogResponse)
                            .toList(),
                    page.getTotalPages(),
                    page.getTotalElements(),
                    page.getSize(),
                    page.getNumber(),
                    page.isLast(),
                    page.hasNext()
            );
        } catch (Exception e) {
            log.error("GET_USAGE_LOGS FAILED workspaceId={} reason={}", workspaceId, e.getMessage());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public PagedResponse<PaymentResponse> getPayments(
            UUID workspaceId,
            PaymentFilterRequest filter
    ) {
        log.info("GET_PAYMENTS START workspaceId={}", workspaceId);

        try {
            Instant startDate = filter.getStartDate() != null
                    ? filter.getStartDate().atStartOfDay(ZoneOffset.UTC).toInstant()
                    : null;
            Instant endDate = filter.getEndDate() != null
                    ? filter.getEndDate().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
                    : null;

            Page<Payment> page = paymentRepository.findByWorkspaceIdWithFilters(
                    workspaceId,
                    startDate,
                    endDate,
                    filter.getStatus(),
                    PaginationUtil.getPageable(filter)
            );

            log.info("GET_PAYMENTS END workspaceId={} totalElements={}", workspaceId, page.getTotalElements());
            return new PagedResponse<>(
                    page.getContent().stream()
                            .map(billingMapper::toPaymentResponse)
                            .toList(),
                    page.getTotalPages(),
                    page.getTotalElements(),
                    page.getSize(),
                    page.getNumber(),
                    page.isLast(),
                    page.hasNext()
            );
        } catch (Exception e) {
            log.error("GET_PAYMENTS FAILED workspaceId={} reason={}", workspaceId, e.getMessage());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public UsageAggregationResponse getUsageAggregations(
            UUID workspaceId,
            UsageAggregationRequest request
    ) {
        log.info("GET_USAGE_AGGREGATIONS START workspaceId={} granularity={}", workspaceId, request.getGranularity());

        try {
            Instant startDate = request.getStartDate().atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant endDate = request.getEndDate().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

            List<UsageLog> logs = usageLogRepository.findByWorkspaceIdAndDateRange(
                    workspaceId,
                    startDate,
                    endDate
            );

            List<UsageAggregationResponse.TimeBasedUsageAggregation> timeBasedUsage =
                    aggregateByTime(logs, request.getGranularity());

            List<UsageAggregationResponse.ModelBasedUsageAggregation> modelBasedUsage =
                    aggregateByModel(logs);

            BigDecimal totalCostCredits = logs.stream()
                    .map(UsageLog::getCostCredits)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long totalTokensIn = logs.stream().mapToLong(UsageLog::getTokensIn).sum();
            long totalTokensOut = logs.stream().mapToLong(UsageLog::getTokensOut).sum();
            long totalRequests = logs.size();

            log.info("GET_USAGE_AGGREGATIONS END workspaceId={} totalRequests={}", workspaceId, totalRequests);
            return new UsageAggregationResponse(
                    timeBasedUsage,
                    modelBasedUsage,
                    totalCostCredits,
                    totalTokensIn,
                    totalTokensOut,
                    totalRequests
            );
        } catch (Exception e) {
            log.error("GET_USAGE_AGGREGATIONS FAILED workspaceId={} reason={}", workspaceId, e.getMessage());
            throw e;
        }
    }

    private List<UsageAggregationResponse.TimeBasedUsageAggregation> aggregateByTime(
            List<UsageLog> logs,
            AggregationGranularity granularity
    ) {
        Map<LocalDate, List<UsageLog>> grouped = logs.stream()
                .collect(Collectors.groupingBy(log -> {
                    LocalDate date = log.getOccurredAt().atZone(ZoneOffset.UTC).toLocalDate();
                    return switch (granularity) {
                        case DAILY -> date;
                        case WEEKLY -> date.with(java.time.DayOfWeek.MONDAY);
                        case MONTHLY -> date.withDayOfMonth(1);
                    };
                }));

        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    List<UsageLog> periodLogs = entry.getValue();
                    return new UsageAggregationResponse.TimeBasedUsageAggregation(
                            entry.getKey(),
                            periodLogs.stream().mapToLong(UsageLog::getTokensIn).sum(),
                            periodLogs.stream().mapToLong(UsageLog::getTokensOut).sum(),
                            periodLogs.stream()
                                    .map(UsageLog::getCostCredits)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add),
                            periodLogs.size()
                    );
                })
                .toList();
    }

    private List<UsageAggregationResponse.ModelBasedUsageAggregation> aggregateByModel(
            List<UsageLog> logs
    ) {
        Map<String, List<UsageLog>> grouped = logs.stream()
                .collect(Collectors.groupingBy(log ->
                        log.getProvider() + "|" + log.getModel()));

        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    List<UsageLog> modelLogs = entry.getValue();
                    String[] parts = entry.getKey().split("\\|", 2);
                    return new UsageAggregationResponse.ModelBasedUsageAggregation(
                            parts[0],
                            parts.length > 1 ? parts[1] : "",
                            modelLogs.stream().mapToLong(UsageLog::getTokensIn).sum(),
                            modelLogs.stream().mapToLong(UsageLog::getTokensOut).sum(),
                            modelLogs.stream()
                                    .map(UsageLog::getCostCredits)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add),
                            modelLogs.size()
                    );
                })
                .toList();
    }

    private SubscriptionUsageCounter createEmptyCounter(
            UUID subscriptionId,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {
        return SubscriptionUsageCounter.builder()
                .subscriptionId(subscriptionId)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .metric(UsageMetric.CREDITS)
                .usedValue(BigDecimal.ZERO)
                .reservedBudget(BigDecimal.ZERO)
                .build();
    }
}
