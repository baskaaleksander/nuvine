package com.baskaaleksander.nuvine.application.mapper;

import com.baskaaleksander.nuvine.application.dto.*;
import com.baskaaleksander.nuvine.domain.model.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BillingMapper {

    public UsageLogResponse toUsageLogResponse(UsageLog usageLog) {
        return new UsageLogResponse(
                usageLog.getId(),
                usageLog.getUserId(),
                usageLog.getConversationId(),
                usageLog.getMessageId(),
                usageLog.getProvider(),
                usageLog.getModel(),
                usageLog.getTokensIn(),
                usageLog.getTokensOut(),
                usageLog.getCostCredits(),
                usageLog.getOccurredAt()
        );
    }

    public PaymentResponse toPaymentResponse(Payment payment) {
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

    public PlanSummaryResponse toPlanSummaryResponse(Plan plan) {
        return new PlanSummaryResponse(
                plan.getId(),
                plan.getCode(),
                plan.getName(),
                plan.getBillingPeriod(),
                plan.getIncludedCredits()
        );
    }

    public UsageSummaryResponse toUsageSummaryResponse(
            SubscriptionUsageCounter counter,
            long includedCredits
    ) {
        BigDecimal usedCredits = counter.getUsedValue();
        BigDecimal reservedCredits = counter.getReservedBudget();
        BigDecimal remaining = BigDecimal.valueOf(includedCredits)
                .subtract(usedCredits)
                .subtract(reservedCredits);

        return new UsageSummaryResponse(
                usedCredits,
                reservedCredits,
                includedCredits,
                remaining,
                counter.getPeriodStart(),
                counter.getPeriodEnd()
        );
    }

    public SubscriptionStatusResponse toSubscriptionStatusResponse(
            Subscription subscription,
            Plan plan,
            SubscriptionUsageCounter counter
    ) {
        PlanSummaryResponse planSummary = toPlanSummaryResponse(plan);
        UsageSummaryResponse usageSummary = toUsageSummaryResponse(counter, plan.getIncludedCredits());

        return new SubscriptionStatusResponse(
                subscription.getId(),
                subscription.getStatus(),
                subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd(),
                subscription.getCancelAtPeriodEnd(),
                planSummary,
                usageSummary
        );
    }
}
