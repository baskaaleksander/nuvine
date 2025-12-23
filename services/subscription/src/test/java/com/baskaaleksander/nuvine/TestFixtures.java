package com.baskaaleksander.nuvine;

import com.baskaaleksander.nuvine.application.dto.CheckLimitRequest;
import com.baskaaleksander.nuvine.application.dto.UserInternalResponse;
import com.baskaaleksander.nuvine.application.dto.WorkspaceInternalSubscriptionResponse;
import com.baskaaleksander.nuvine.application.dto.WorkspaceMemberResponse;
import com.baskaaleksander.nuvine.domain.model.*;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DlqMessage;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.LogTokenUsageEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.PaymentActionRequiredEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

public final class TestFixtures {

    private TestFixtures() {
    }


    public static final UUID DEFAULT_WORKSPACE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID DEFAULT_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID DEFAULT_SUBSCRIPTION_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    public static final UUID DEFAULT_PLAN_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    public static final UUID DEFAULT_CONVERSATION_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    public static final UUID DEFAULT_MESSAGE_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    public static final String DEFAULT_STRIPE_CUSTOMER_ID = "cus_test123";
    public static final String DEFAULT_STRIPE_SUBSCRIPTION_ID = "sub_test123";
    public static final String DEFAULT_STRIPE_PRICE_ID = "price_test123";
    public static final String DEFAULT_STRIPE_SESSION_ID = "cs_test123";

    public static final String DEFAULT_PROVIDER_KEY = "openai";
    public static final String DEFAULT_MODEL_KEY = "gpt-4";


    public static UUID randomId() {
        return UUID.randomUUID();
    }

    public static Subscription.SubscriptionBuilder activeSubscription() {
        Instant now = Instant.now();
        return Subscription.builder()
                .id(DEFAULT_SUBSCRIPTION_ID)
                .workspaceId(DEFAULT_WORKSPACE_ID)
                .planId(DEFAULT_PLAN_ID)
                .stripeCustomerId(DEFAULT_STRIPE_CUSTOMER_ID)
                .stripeSubscriptionId(DEFAULT_STRIPE_SUBSCRIPTION_ID)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(now.minus(15, ChronoUnit.DAYS))
                .currentPeriodEnd(now.plus(15, ChronoUnit.DAYS))
                .cancelAtPeriodEnd(false)
                .createdAt(now.minus(30, ChronoUnit.DAYS))
                .updatedAt(now);
    }

    public static Subscription.SubscriptionBuilder pastDueSubscription() {
        return activeSubscription()
                .status(SubscriptionStatus.PAST_DUE);
    }

    public static Subscription.SubscriptionBuilder canceledSubscription() {
        return activeSubscription()
                .status(SubscriptionStatus.CANCELED)
                .cancelAtPeriodEnd(true);
    }

    public static Plan.PlanBuilder freePlan() {
        return Plan.builder()
                .id(DEFAULT_PLAN_ID)
                .code("FREE")
                .name("Free Plan")
                .stripePriceId(DEFAULT_STRIPE_PRICE_ID)
                .billingPeriod(BillingPeriod.MONTHLY)
                .includedCredits(1000L)
                .maxStorageSize(104857600L) // 100MB
                .hardLimitBehaviour(HardLimitBehaviour.BLOCK)
                .createdAt(Instant.now())
                .updatedAt(Instant.now());
    }

    public static Plan.PlanBuilder proPlan() {
        return Plan.builder()
                .id(randomId())
                .code("PRO")
                .name("Pro Plan")
                .stripePriceId("price_pro_test123")
                .billingPeriod(BillingPeriod.MONTHLY)
                .includedCredits(50000L)
                .maxStorageSize(1073741824L) // 1GB
                .hardLimitBehaviour(HardLimitBehaviour.SOFT_BLOCK)
                .createdAt(Instant.now())
                .updatedAt(Instant.now());
    }

    public static Plan.PlanBuilder maxPlan() {
        return Plan.builder()
                .id(randomId())
                .code("MAX")
                .name("Max Plan")
                .stripePriceId("price_max_test123")
                .billingPeriod(BillingPeriod.MONTHLY)
                .includedCredits(200000L)
                .maxStorageSize(10737418240L) // 10GB
                .hardLimitBehaviour(HardLimitBehaviour.ALLOW_WITH_OVERAGE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now());
    }

    public static SubscriptionUsageCounter.SubscriptionUsageCounterBuilder usageCounter() {
        LocalDate now = LocalDate.now();
        return SubscriptionUsageCounter.builder()
                .id(randomId())
                .subscriptionId(DEFAULT_SUBSCRIPTION_ID)
                .periodStart(now.withDayOfMonth(1))
                .periodEnd(now.withDayOfMonth(1).plusMonths(1).minusDays(1))
                .metric(UsageMetric.CREDITS)
                .usedValue(BigDecimal.valueOf(500))
                .reservedBudget(BigDecimal.ZERO)
                .createdAt(Instant.now())
                .updatedAt(Instant.now());
    }

    public static SubscriptionUsageCounter.SubscriptionUsageCounterBuilder emptyUsageCounter() {
        return usageCounter()
                .usedValue(BigDecimal.ZERO)
                .reservedBudget(BigDecimal.ZERO);
    }

    public static SubscriptionUsageCounter.SubscriptionUsageCounterBuilder usageCounterWithReservation() {
        return usageCounter()
                .usedValue(BigDecimal.valueOf(500))
                .reservedBudget(BigDecimal.valueOf(100));
    }


    public static UsageLog.UsageLogBuilder usageLog() {
        Instant now = Instant.now();
        return UsageLog.builder()
                .id(randomId())
                .subscriptionId(DEFAULT_SUBSCRIPTION_ID)
                .workspaceId(DEFAULT_WORKSPACE_ID)
                .userId(DEFAULT_USER_ID)
                .conversationId(DEFAULT_CONVERSATION_ID)
                .messageId(DEFAULT_MESSAGE_ID)
                .sourceService("chat-service")
                .provider(DEFAULT_PROVIDER_KEY)
                .model(DEFAULT_MODEL_KEY)
                .tokensIn(1000L)
                .tokensOut(500L)
                .costCredits(BigDecimal.valueOf(0.045))
                .createdAt(now)
                .occurredAt(now);
    }


    public static LlmProvider.LlmProviderBuilder llmProvider() {
        return LlmProvider.builder()
                .id(randomId())
                .providerKey(DEFAULT_PROVIDER_KEY)
                .displayName("OpenAI")
                .active(true);
    }

    public static ModelPricing modelPricing() {
        ModelPricing pricing = new ModelPricing();
        pricing.setInputPricePer1MTokens(BigDecimal.valueOf(30.00));
        pricing.setOutputPricePer1MTokens(BigDecimal.valueOf(60.00));
        pricing.setCurrency("USD");
        return pricing;
    }

    public static ModelPricing cheapModelPricing() {
        ModelPricing pricing = new ModelPricing();
        pricing.setInputPricePer1MTokens(BigDecimal.valueOf(0.50));
        pricing.setOutputPricePer1MTokens(BigDecimal.valueOf(1.50));
        pricing.setCurrency("USD");
        return pricing;
    }

    public static LlmModel.LlmModelBuilder llmModel() {
        return LlmModel.builder()
                .id(randomId())
                .provider(llmProvider().build())
                .modelKey(DEFAULT_MODEL_KEY)
                .displayName("GPT-4")
                .maxOutputTokens(4096L)
                .pricing(modelPricing())
                .free(false)
                .active(true)
                .effectiveFrom(Instant.now().minus(30, ChronoUnit.DAYS))
                .effectiveTo(null);
    }

    public static LlmModel.LlmModelBuilder freeModel() {
        return llmModel()
                .modelKey("gpt-3.5-turbo")
                .displayName("GPT-3.5 Turbo")
                .free(true)
                .pricing(cheapModelPricing());
    }

    public static Payment.PaymentBuilder payment() {
        Instant now = Instant.now();
        return Payment.builder()
                .id(randomId())
                .workspaceId(DEFAULT_WORKSPACE_ID)
                .subscriptionId(DEFAULT_SUBSCRIPTION_ID)
                .stripeInvoiceId("in_test123")
                .stripePaymentIntentId("pi_test123")
                .amountDue(BigDecimal.valueOf(29.99))
                .amountPaid(BigDecimal.valueOf(29.99))
                .currency("USD")
                .status(PaymentStatus.SUCCEEDED)
                .billingPeriodStart(now.minus(30, ChronoUnit.DAYS))
                .billingPeriodEnd(now)
                .invoicePdfUrl("https://stripe.com/invoice/test123.pdf")
                .description("Pro Plan - Monthly")
                .createdAt(now)
                .updatedAt(now);
    }

    public static Payment.PaymentBuilder pendingPayment() {
        return payment()
                .status(PaymentStatus.PENDING)
                .amountPaid(BigDecimal.ZERO);
    }

    public static Payment.PaymentBuilder failedPayment() {
        return payment()
                .status(PaymentStatus.FAILED)
                .amountPaid(BigDecimal.ZERO);
    }

    public static PaymentSession.PaymentSessionBuilder paymentSession() {
        Instant now = Instant.now();
        return PaymentSession.builder()
                .id(randomId())
                .workspaceId(DEFAULT_WORKSPACE_ID)
                .userId(DEFAULT_USER_ID)
                .planId(DEFAULT_PLAN_ID)
                .type(PaymentSessionType.PAYMENT)
                .intent(PaymentSessionIntent.SUBSCRIPTION_CREATE)
                .stripeSessionId(DEFAULT_STRIPE_SESSION_ID)
                .stripeUrl("https://checkout.stripe.com/test123")
                .status(PaymentSessionStatus.PENDING)
                .createdAt(now)
                .expiresAt(now.plus(24, ChronoUnit.HOURS))
                .metadataJson(Map.of(
                        "workspace_id", DEFAULT_WORKSPACE_ID.toString(),
                        "plan_id", DEFAULT_PLAN_ID.toString(),
                        "user_id", DEFAULT_USER_ID.toString()
                ));
    }

    public static PaymentSession.PaymentSessionBuilder completedPaymentSession() {
        return paymentSession()
                .status(PaymentSessionStatus.COMPLETED)
                .completedAt(Instant.now());
    }

    public static PaymentSession.PaymentSessionBuilder updatePaymentSession() {
        return paymentSession()
                .intent(PaymentSessionIntent.SUBSCRIPTION_UPDATE);
    }

    public static LogTokenUsageEvent logTokenUsageEvent() {
        return new LogTokenUsageEvent(
                DEFAULT_WORKSPACE_ID.toString(),
                DEFAULT_USER_ID.toString(),
                DEFAULT_CONVERSATION_ID.toString(),
                DEFAULT_MESSAGE_ID.toString(),
                DEFAULT_MODEL_KEY,
                DEFAULT_PROVIDER_KEY,
                "chat-service",
                1000L,
                500L,
                Instant.now()
        );
    }

    public static LogTokenUsageEvent logTokenUsageEvent(long tokensIn, long tokensOut) {
        return new LogTokenUsageEvent(
                DEFAULT_WORKSPACE_ID.toString(),
                DEFAULT_USER_ID.toString(),
                DEFAULT_CONVERSATION_ID.toString(),
                DEFAULT_MESSAGE_ID.toString(),
                DEFAULT_MODEL_KEY,
                DEFAULT_PROVIDER_KEY,
                "chat-service",
                tokensIn,
                tokensOut,
                Instant.now()
        );
    }

    public static DlqMessage dlqMessage() {
        return DlqMessage.createInitial(
                logTokenUsageEvent(),
                new RuntimeException("Test error"),
                "log-token-usage-topic"
        );
    }

    public static DlqMessage dlqMessage(int attemptCount) {
        DlqMessage initial = dlqMessage();
        DlqMessage result = initial;
        for (int i = 1; i < attemptCount; i++) {
            result = result.incrementAttempt(new RuntimeException("Test error " + i));
        }
        return result;
    }

    public static PaymentActionRequiredEvent paymentActionRequiredEvent() {
        return new PaymentActionRequiredEvent(
                "owner@example.com",
                "in_test123",
                "https://stripe.com/invoice/test123",
                DEFAULT_WORKSPACE_ID.toString(),
                "Test Workspace",
                DEFAULT_USER_ID.toString()
        );
    }

    public static CheckLimitRequest checkLimitRequest() {
        return new CheckLimitRequest(
                DEFAULT_WORKSPACE_ID,
                DEFAULT_MODEL_KEY,
                DEFAULT_PROVIDER_KEY,
                1000L
        );
    }

    public static CheckLimitRequest checkLimitRequest(long inputTokens) {
        return new CheckLimitRequest(
                DEFAULT_WORKSPACE_ID,
                DEFAULT_MODEL_KEY,
                DEFAULT_PROVIDER_KEY,
                inputTokens
        );
    }

    public static UserInternalResponse userInternalResponse() {
        return new UserInternalResponse(
                DEFAULT_USER_ID,
                "John",
                "Doe",
                "john.doe@example.com"
        );
    }

    public static UserInternalResponse userInternalResponse(UUID userId) {
        return new UserInternalResponse(
                userId,
                "John",
                "Doe",
                "john.doe@example.com"
        );
    }

    public static WorkspaceInternalSubscriptionResponse workspaceInternalResponse() {
        return new WorkspaceInternalSubscriptionResponse(
                DEFAULT_WORKSPACE_ID,
                "Test Workspace",
                BillingTier.FREE,
                DEFAULT_STRIPE_SUBSCRIPTION_ID,
                DEFAULT_USER_ID
        );
    }

    public static WorkspaceInternalSubscriptionResponse workspaceInternalResponse(UUID ownerId) {
        return new WorkspaceInternalSubscriptionResponse(
                DEFAULT_WORKSPACE_ID,
                "Test Workspace",
                BillingTier.FREE,
                DEFAULT_STRIPE_SUBSCRIPTION_ID,
                ownerId
        );
    }

    public static WorkspaceMemberResponse ownerMemberResponse() {
        return new WorkspaceMemberResponse(
                randomId(),
                DEFAULT_WORKSPACE_ID,
                DEFAULT_USER_ID,
                WorkspaceRole.OWNER,
                Instant.now()
        );
    }

    public static WorkspaceMemberResponse memberMemberResponse() {
        return new WorkspaceMemberResponse(
                randomId(),
                DEFAULT_WORKSPACE_ID,
                DEFAULT_USER_ID,
                WorkspaceRole.MEMBER,
                Instant.now()
        );
    }

    public static WorkspaceMemberResponse moderatorMemberResponse() {
        return new WorkspaceMemberResponse(
                randomId(),
                DEFAULT_WORKSPACE_ID,
                DEFAULT_USER_ID,
                WorkspaceRole.MODERATOR,
                Instant.now()
        );
    }

    public static WorkspaceMemberResponse viewerMemberResponse() {
        return new WorkspaceMemberResponse(
                randomId(),
                DEFAULT_WORKSPACE_ID,
                DEFAULT_USER_ID,
                WorkspaceRole.VIEWER,
                Instant.now()
        );
    }
}
