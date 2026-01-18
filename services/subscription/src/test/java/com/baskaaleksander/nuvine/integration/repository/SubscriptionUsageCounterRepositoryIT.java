package com.baskaaleksander.nuvine.integration.repository;

import com.baskaaleksander.nuvine.domain.model.*;
import com.baskaaleksander.nuvine.infrastructure.persistence.SubscriptionUsageCounterRepository;
import com.baskaaleksander.nuvine.integration.base.BaseRepositoryIntegrationTest;
import com.baskaaleksander.nuvine.integration.support.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionUsageCounterRepositoryIT extends BaseRepositoryIntegrationTest {

    @Autowired
    private SubscriptionUsageCounterRepository usageCounterRepository;

    @Autowired
    private TestDataBuilder testDataBuilder;

    private Subscription subscription;
    private LocalDate periodStart;
    private LocalDate periodEnd;

    @BeforeEach
    void setUp() {
        testDataBuilder.cleanUp();
        Plan plan = testDataBuilder.createProPlan();
        subscription = testDataBuilder.createActiveSubscription(UUID.randomUUID(), plan);
        periodStart = LocalDate.now().withDayOfMonth(1);
        periodEnd = periodStart.plusMonths(1).minusDays(1);
    }

    @Nested
    @DisplayName("findCurrentSubscriptionUsageCounter")
    class FindCurrentSubscriptionUsageCounter {

        @Test
        @DisplayName("Should return counter within period")
        void shouldReturnCounterWithinPeriod() {
            SubscriptionUsageCounter counter = testDataBuilder.createUsageCounter(
                    subscription, periodStart, periodEnd, new BigDecimal("100"), BigDecimal.ZERO);

            Optional<SubscriptionUsageCounter> result = usageCounterRepository
                    .findCurrentSubscriptionUsageCounter(
                            subscription.getId(), periodStart, periodEnd, UsageMetric.CREDITS);

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(counter.getId());
            assertThat(result.get().getUsedValue()).isEqualByComparingTo(new BigDecimal("100"));
        }

        @Test
        @DisplayName("Should return empty when period does not match")
        void shouldReturnEmptyWhenPeriodDoesNotMatch() {
            testDataBuilder.createUsageCounter(
                    subscription, periodStart, periodEnd, new BigDecimal("100"), BigDecimal.ZERO);

            LocalDate differentPeriodStart = periodStart.minusMonths(1);
            LocalDate differentPeriodEnd = periodEnd.minusMonths(1);

            Optional<SubscriptionUsageCounter> result = usageCounterRepository
                    .findCurrentSubscriptionUsageCounter(
                            subscription.getId(), differentPeriodStart, differentPeriodEnd, UsageMetric.CREDITS);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty when metric does not match")
        void shouldReturnEmptyWhenMetricDoesNotMatch() {
            testDataBuilder.createUsageCounter(
                    subscription, periodStart, periodEnd, new BigDecimal("100"), BigDecimal.ZERO);

            Optional<SubscriptionUsageCounter> result = usageCounterRepository
                    .findCurrentSubscriptionUsageCounter(
                            subscription.getId(), periodStart, periodEnd, UsageMetric.STORAGE);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("incrementReservedBudget")
    class IncrementReservedBudget {

        @Test
        @DisplayName("Should atomically increment reserved budget")
        @Transactional
        void shouldAtomicallyIncrementReservedBudget() {
            testDataBuilder.createUsageCounter(
                    subscription, periodStart, periodEnd, new BigDecimal("100"), new BigDecimal("50"));

            int rowsUpdated = usageCounterRepository.incrementReservedBudget(
                    subscription.getId(), periodStart, periodEnd, UsageMetric.CREDITS, new BigDecimal("25"));

            assertThat(rowsUpdated).isEqualTo(1);

            Optional<SubscriptionUsageCounter> updated = usageCounterRepository
                    .findCurrentSubscriptionUsageCounter(
                            subscription.getId(), periodStart, periodEnd, UsageMetric.CREDITS);

            assertThat(updated).isPresent();
            assertThat(updated.get().getReservedBudget()).isEqualByComparingTo(new BigDecimal("75"));
        }

        @Test
        @DisplayName("Should return 0 when counter not found")
        @Transactional
        void shouldReturnZeroWhenCounterNotFound() {
            int rowsUpdated = usageCounterRepository.incrementReservedBudget(
                    UUID.randomUUID(), periodStart, periodEnd, UsageMetric.CREDITS, new BigDecimal("25"));

            assertThat(rowsUpdated).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("decrementReservedBudget")
    class DecrementReservedBudget {

        @Test
        @DisplayName("Should atomically decrement reserved budget")
        @Transactional
        void shouldAtomicallyDecrementReservedBudget() {
            testDataBuilder.createUsageCounter(
                    subscription, periodStart, periodEnd, new BigDecimal("100"), new BigDecimal("50"));

            int rowsUpdated = usageCounterRepository.decrementReservedBudget(
                    subscription.getId(), periodStart, periodEnd, UsageMetric.CREDITS, new BigDecimal("20"));

            assertThat(rowsUpdated).isEqualTo(1);

            Optional<SubscriptionUsageCounter> updated = usageCounterRepository
                    .findCurrentSubscriptionUsageCounter(
                            subscription.getId(), periodStart, periodEnd, UsageMetric.CREDITS);

            assertThat(updated).isPresent();
            assertThat(updated.get().getReservedBudget()).isEqualByComparingTo(new BigDecimal("30"));
        }

        @Test
        @DisplayName("Should return 0 when counter not found")
        @Transactional
        void shouldReturnZeroWhenCounterNotFound() {
            int rowsUpdated = usageCounterRepository.decrementReservedBudget(
                    UUID.randomUUID(), periodStart, periodEnd, UsageMetric.CREDITS, new BigDecimal("20"));

            assertThat(rowsUpdated).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("incrementUsage")
    class IncrementUsage {

        @Test
        @DisplayName("Should atomically increment used value")
        @Transactional
        void shouldAtomicallyIncrementUsedValue() {
            testDataBuilder.createUsageCounter(
                    subscription, periodStart, periodEnd, new BigDecimal("100"), BigDecimal.ZERO);

            int rowsUpdated = usageCounterRepository.incrementUsage(
                    subscription.getId(), periodStart, periodEnd, UsageMetric.CREDITS, new BigDecimal("25"));

            assertThat(rowsUpdated).isEqualTo(1);

            Optional<SubscriptionUsageCounter> updated = usageCounterRepository
                    .findCurrentSubscriptionUsageCounter(
                            subscription.getId(), periodStart, periodEnd, UsageMetric.CREDITS);

            assertThat(updated).isPresent();
            assertThat(updated.get().getUsedValue()).isEqualByComparingTo(new BigDecimal("125"));
        }
    }
}
