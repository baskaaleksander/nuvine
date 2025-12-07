package com.baskaaleksander.nuvine.infrastructure.persistence;

import com.baskaaleksander.nuvine.domain.model.SubscriptionUsageCounter;
import com.baskaaleksander.nuvine.domain.model.UsageMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionUsageCounterRepository extends JpaRepository<SubscriptionUsageCounter, UUID> {

    @Modifying
    @Query("""
            UPDATE SubscriptionUsageCounter 
            SET usedValue = usedValue + :cost,
                updatedAt = CURRENT_TIMESTAMP
            WHERE subscriptionId = :subscriptionId 
            AND periodStart = :periodStart 
            AND periodEnd = :periodEnd 
            AND metric = :metric
            """)
    int incrementUsage(
            @Param("subscriptionId") UUID subscriptionId,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd,
            @Param("metric") UsageMetric metric,
            @Param("cost") BigDecimal cost
    );

    @Query("""
            SELECT c FROM SubscriptionUsageCounter c
            WHERE c.subscriptionId = :subscriptionId
            AND c.periodStart = :periodStart
            AND c.periodEnd = :periodEnd
            AND c.metric = :metric
            """)
    Optional<SubscriptionUsageCounter> findCurrentSubscriptionUsageCounter(
            @Param("subscriptionId") UUID subscriptionId,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd,
            @Param("metric") UsageMetric metric
    );
}