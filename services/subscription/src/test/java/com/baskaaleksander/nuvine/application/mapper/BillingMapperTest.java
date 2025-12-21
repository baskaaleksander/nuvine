package com.baskaaleksander.nuvine.application.mapper;

import com.baskaaleksander.nuvine.TestFixtures;
import com.baskaaleksander.nuvine.application.dto.*;
import com.baskaaleksander.nuvine.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BillingMapper")
class BillingMapperTest {

    private BillingMapper billingMapper;

    @BeforeEach
    void setUp() {
        billingMapper = new BillingMapper();
    }

    @Test
    @DisplayName("toUsageLogResponse maps all fields correctly")
    void toUsageLogResponse_mapsAllFields() {
        UsageLog usageLog = TestFixtures.usageLog().build();

        UsageLogResponse response = billingMapper.toUsageLogResponse(usageLog);

        assertThat(response.id()).isEqualTo(usageLog.getId());
        assertThat(response.userId()).isEqualTo(usageLog.getUserId());
        assertThat(response.conversationId()).isEqualTo(usageLog.getConversationId());
        assertThat(response.messageId()).isEqualTo(usageLog.getMessageId());
        assertThat(response.provider()).isEqualTo(usageLog.getProvider());
        assertThat(response.model()).isEqualTo(usageLog.getModel());
        assertThat(response.tokensIn()).isEqualTo(usageLog.getTokensIn());
        assertThat(response.tokensOut()).isEqualTo(usageLog.getTokensOut());
        assertThat(response.costCredits()).isEqualTo(usageLog.getCostCredits());
        assertThat(response.occurredAt()).isEqualTo(usageLog.getOccurredAt());
    }

    @Test
    @DisplayName("toPaymentResponse maps all fields correctly")
    void toPaymentResponse_mapsAllFields() {
        Payment payment = TestFixtures.payment().build();

        PaymentResponse response = billingMapper.toPaymentResponse(payment);

        assertThat(response.id()).isEqualTo(payment.getId());
        assertThat(response.amountDue()).isEqualTo(payment.getAmountDue());
        assertThat(response.amountPaid()).isEqualTo(payment.getAmountPaid());
        assertThat(response.currency()).isEqualTo(payment.getCurrency());
        assertThat(response.status()).isEqualTo(payment.getStatus());
        assertThat(response.billingPeriodStart()).isEqualTo(payment.getBillingPeriodStart());
        assertThat(response.billingPeriodEnd()).isEqualTo(payment.getBillingPeriodEnd());
        assertThat(response.invoicePdfUrl()).isEqualTo(payment.getInvoicePdfUrl());
        assertThat(response.description()).isEqualTo(payment.getDescription());
        assertThat(response.createdAt()).isEqualTo(payment.getCreatedAt());
    }

    @Test
    @DisplayName("toPlanSummaryResponse maps all fields correctly")
    void toPlanSummaryResponse_mapsAllFields() {
        Plan plan = TestFixtures.proPlan().build();

        PlanSummaryResponse response = billingMapper.toPlanSummaryResponse(plan);

        assertThat(response.id()).isEqualTo(plan.getId());
        assertThat(response.code()).isEqualTo(plan.getCode());
        assertThat(response.name()).isEqualTo(plan.getName());
        assertThat(response.billingPeriod()).isEqualTo(plan.getBillingPeriod());
        assertThat(response.includedCredits()).isEqualTo(plan.getIncludedCredits());
    }

    @Test
    @DisplayName("toUsageSummaryResponse calculates remaining credits correctly")
    void toUsageSummaryResponse_calculatesRemainingCredits() {
        SubscriptionUsageCounter counter = TestFixtures.usageCounterWithReservation().build();
        long includedCredits = 1000L;

        BigDecimal expectedRemaining = BigDecimal.valueOf(includedCredits)
                .subtract(counter.getUsedValue())
                .subtract(counter.getReservedBudget());

        UsageSummaryResponse response = billingMapper.toUsageSummaryResponse(counter, includedCredits);

        assertThat(response.usedCredits()).isEqualTo(counter.getUsedValue());
        assertThat(response.reservedCredits()).isEqualTo(counter.getReservedBudget());
        assertThat(response.includedCredits()).isEqualTo(includedCredits);
        assertThat(response.remainingCredits()).isEqualByComparingTo(expectedRemaining);
        assertThat(response.periodStart()).isEqualTo(counter.getPeriodStart());
        assertThat(response.periodEnd()).isEqualTo(counter.getPeriodEnd());
    }

    @Test
    @DisplayName("toSubscriptionStatusResponse combines all components correctly")
    void toSubscriptionStatusResponse_combinesAllComponents() {
        Subscription subscription = TestFixtures.activeSubscription().build();
        Plan plan = TestFixtures.freePlan().build();
        SubscriptionUsageCounter counter = TestFixtures.usageCounter().build();

        SubscriptionStatusResponse response = billingMapper.toSubscriptionStatusResponse(
                subscription, plan, counter);

        assertThat(response.id()).isEqualTo(subscription.getId());
        assertThat(response.status()).isEqualTo(subscription.getStatus());
        assertThat(response.currentPeriodStart()).isEqualTo(subscription.getCurrentPeriodStart());
        assertThat(response.currentPeriodEnd()).isEqualTo(subscription.getCurrentPeriodEnd());
        assertThat(response.cancelAtPeriodEnd()).isEqualTo(subscription.getCancelAtPeriodEnd());

        assertThat(response.plan()).isNotNull();
        assertThat(response.plan().id()).isEqualTo(plan.getId());
        assertThat(response.plan().code()).isEqualTo(plan.getCode());
        assertThat(response.plan().name()).isEqualTo(plan.getName());
        assertThat(response.plan().billingPeriod()).isEqualTo(plan.getBillingPeriod());
        assertThat(response.plan().includedCredits()).isEqualTo(plan.getIncludedCredits());

        assertThat(response.usage()).isNotNull();
        assertThat(response.usage().usedCredits()).isEqualTo(counter.getUsedValue());
        assertThat(response.usage().reservedCredits()).isEqualTo(counter.getReservedBudget());
        assertThat(response.usage().includedCredits()).isEqualTo(plan.getIncludedCredits());

        BigDecimal expectedRemaining = BigDecimal.valueOf(plan.getIncludedCredits())
                .subtract(counter.getUsedValue())
                .subtract(counter.getReservedBudget());
        assertThat(response.usage().remainingCredits()).isEqualByComparingTo(expectedRemaining);
    }
}
