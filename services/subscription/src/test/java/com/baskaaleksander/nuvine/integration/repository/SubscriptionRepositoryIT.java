package com.baskaaleksander.nuvine.integration.repository;

import com.baskaaleksander.nuvine.domain.model.*;
import com.baskaaleksander.nuvine.infrastructure.persistence.SubscriptionRepository;
import com.baskaaleksander.nuvine.integration.base.BaseRepositoryIntegrationTest;
import com.baskaaleksander.nuvine.integration.support.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionRepositoryIT extends BaseRepositoryIntegrationTest {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private TestDataBuilder testDataBuilder;

    private Plan plan;

    @BeforeEach
    void setUp() {
        testDataBuilder.cleanUp();
        plan = testDataBuilder.createProPlan();
    }

    @Nested
    @DisplayName("findByWorkspaceId")
    class FindByWorkspaceId {

        @Test
        @DisplayName("Should return subscription when exists")
        void shouldReturnSubscriptionWhenExists() {
            UUID workspaceId = UUID.randomUUID();
            Subscription subscription = testDataBuilder.createActiveSubscription(workspaceId, plan);

            Optional<Subscription> result = subscriptionRepository.findByWorkspaceId(workspaceId);

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(subscription.getId());
            assertThat(result.get().getWorkspaceId()).isEqualTo(workspaceId);
            assertThat(result.get().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should return empty when not exists")
        void shouldReturnEmptyWhenNotExists() {
            UUID nonExistentWorkspaceId = UUID.randomUUID();

            Optional<Subscription> result = subscriptionRepository.findByWorkspaceId(nonExistentWorkspaceId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByStripeSubscriptionId")
    class FindByStripeSubscriptionId {

        @Test
        @DisplayName("Should return subscription when exists")
        void shouldReturnSubscriptionWhenExists() {
            UUID workspaceId = UUID.randomUUID();
            Subscription subscription = testDataBuilder.createActiveSubscription(workspaceId, plan);

            Subscription result = subscriptionRepository.findByStripeSubscriptionId(
                    subscription.getStripeSubscriptionId());

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(subscription.getId());
            assertThat(result.getStripeSubscriptionId()).isEqualTo(subscription.getStripeSubscriptionId());
        }

        @Test
        @DisplayName("Should return null when not exists")
        void shouldReturnNullWhenNotExists() {
            Subscription result = subscriptionRepository.findByStripeSubscriptionId("sub_nonexistent");

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("findByStripeCustomerId")
    class FindByStripeCustomerId {

        @Test
        @DisplayName("Should return all subscriptions for customer")
        void shouldReturnAllSubscriptionsForCustomer() {
            UUID workspaceId1 = UUID.randomUUID();
            UUID workspaceId2 = UUID.randomUUID();

            Subscription subscription1 = testDataBuilder.createActiveSubscription(workspaceId1, plan);
            Subscription subscription2 = testDataBuilder.createSubscription(workspaceId2, plan, SubscriptionStatus.CANCELED);

            String sharedCustomerId = subscription1.getStripeCustomerId();
            subscription2.setStripeCustomerId(sharedCustomerId);
            subscriptionRepository.save(subscription2);

            List<Subscription> results = subscriptionRepository.findByStripeCustomerId(sharedCustomerId);

            assertThat(results).hasSize(2);
            assertThat(results).extracting(Subscription::getStripeCustomerId)
                    .containsOnly(sharedCustomerId);
        }

        @Test
        @DisplayName("Should return empty list when customer has no subscriptions")
        void shouldReturnEmptyListWhenNoSubscriptions() {
            List<Subscription> results = subscriptionRepository.findByStripeCustomerId("cus_nonexistent");

            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("Subscription Status Tests")
    class SubscriptionStatusTests {

        @Test
        @DisplayName("Should correctly persist different subscription statuses")
        void shouldPersistDifferentStatuses() {
            UUID workspaceId = UUID.randomUUID();
            Subscription subscription = testDataBuilder.createSubscription(workspaceId, plan, SubscriptionStatus.PAST_DUE);

            Subscription saved = subscriptionRepository.findById(subscription.getId()).orElseThrow();
            assertThat(saved.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);

            subscription.setStatus(SubscriptionStatus.CANCELED);
            subscriptionRepository.save(subscription);

            Subscription updated = subscriptionRepository.findById(subscription.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
        }

        @Test
        @DisplayName("Should persist cancel_at_period_end flag")
        void shouldPersistCancelAtPeriodEnd() {
            UUID workspaceId = UUID.randomUUID();
            Subscription subscription = testDataBuilder.createActiveSubscription(workspaceId, plan);

            assertThat(subscription.getCancelAtPeriodEnd()).isFalse();

            subscription.setCancelAtPeriodEnd(true);
            subscriptionRepository.save(subscription);

            Subscription updated = subscriptionRepository.findById(subscription.getId()).orElseThrow();
            assertThat(updated.getCancelAtPeriodEnd()).isTrue();
        }
    }
}
