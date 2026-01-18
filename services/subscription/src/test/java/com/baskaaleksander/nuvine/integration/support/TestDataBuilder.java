package com.baskaaleksander.nuvine.integration.support;

import com.baskaaleksander.nuvine.domain.model.*;
import com.baskaaleksander.nuvine.infrastructure.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Component
public class TestDataBuilder {

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentSessionRepository paymentSessionRepository;

    @Autowired
    private UsageLogRepository usageLogRepository;

    @Autowired
    private SubscriptionUsageCounterRepository usageCounterRepository;

    @Autowired
    private LlmModelRepository llmModelRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Plan createFreePlan() {
        Instant now = Instant.now();
        Plan plan = Plan.builder()
                .code("FREE")
                .name("Free Plan")
                .stripePriceId("price_free_" + UUID.randomUUID().toString().substring(0, 8))
                .billingPeriod(BillingPeriod.MONTHLY)
                .includedCredits(1000L)
                .maxStorageSize(100_000_000L)
                .hardLimitBehaviour(HardLimitBehaviour.BLOCK)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return planRepository.save(plan);
    }

    public Plan createProPlan() {
        Instant now = Instant.now();
        Plan plan = Plan.builder()
                .code("PRO")
                .name("Pro Plan")
                .stripePriceId("price_pro_" + UUID.randomUUID().toString().substring(0, 8))
                .billingPeriod(BillingPeriod.MONTHLY)
                .includedCredits(50000L)
                .maxStorageSize(1_000_000_000L)
                .hardLimitBehaviour(HardLimitBehaviour.SOFT_BLOCK)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return planRepository.save(plan);
    }

    public Plan createMaxPlan() {
        Instant now = Instant.now();
        Plan plan = Plan.builder()
                .code("MAX")
                .name("Max Plan")
                .stripePriceId("price_max_" + UUID.randomUUID().toString().substring(0, 8))
                .billingPeriod(BillingPeriod.MONTHLY)
                .includedCredits(200000L)
                .maxStorageSize(10_000_000_000L)
                .hardLimitBehaviour(HardLimitBehaviour.ALLOW_WITH_OVERAGE)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return planRepository.save(plan);
    }

    public Plan createPlan(String code, String name, BillingPeriod billingPeriod,
                           long includedCredits, HardLimitBehaviour hardLimitBehaviour) {
        Instant now = Instant.now();
        Plan plan = Plan.builder()
                .code(code)
                .name(name)
                .stripePriceId("price_" + code.toLowerCase() + "_" + UUID.randomUUID().toString().substring(0, 8))
                .billingPeriod(billingPeriod)
                .includedCredits(includedCredits)
                .maxStorageSize(1_000_000_000L)
                .hardLimitBehaviour(hardLimitBehaviour)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return planRepository.save(plan);
    }

    public LlmProvider createLlmProvider(String providerKey, String displayName) {
        String sql = """
            INSERT INTO llm_providers (id, provider_key, display_name, active)
            VALUES (?, ?, ?, true)
            """;
        UUID providerId = UUID.randomUUID();
        jdbcTemplate.update(sql, providerId, providerKey, displayName);

        LlmProvider provider = new LlmProvider();
        provider.setId(providerId);
        provider.setProviderKey(providerKey);
        provider.setDisplayName(displayName);
        provider.setActive(true);
        return provider;
    }

    public LlmModel createLlmModel(LlmProvider provider, String modelKey, String displayName) {
        ModelPricing pricing = new ModelPricing();
        pricing.setInputPricePer1MTokens(new BigDecimal("3.00"));
        pricing.setOutputPricePer1MTokens(new BigDecimal("15.00"));
        pricing.setCurrency("USD");

        LlmModel model = LlmModel.builder()
                .provider(provider)
                .modelKey(modelKey)
                .displayName(displayName)
                .maxOutputTokens(4096L)
                .pricing(pricing)
                .free(false)
                .active(true)
                .effectiveFrom(Instant.now().minus(30, ChronoUnit.DAYS))
                .build();
        return llmModelRepository.save(model);
    }

    public Subscription createActiveSubscription(UUID workspaceId, Plan plan) {
        return createSubscription(workspaceId, plan, SubscriptionStatus.ACTIVE);
    }

    public Subscription createSubscription(UUID workspaceId, Plan plan, SubscriptionStatus status) {
        Instant now = Instant.now();
        Subscription subscription = Subscription.builder()
                .workspaceId(workspaceId)
                .planId(plan.getId())
                .stripeCustomerId("cus_" + UUID.randomUUID().toString().substring(0, 14))
                .stripeSubscriptionId("sub_" + UUID.randomUUID().toString().substring(0, 14))
                .status(status)
                .cancelAtPeriodEnd(false)
                .currentPeriodStart(now.minus(15, ChronoUnit.DAYS))
                .currentPeriodEnd(now.plus(15, ChronoUnit.DAYS))
                .build();
        return subscriptionRepository.save(subscription);
    }

    public Payment createPayment(Subscription subscription, PaymentStatus status) {
        Instant now = Instant.now();
        Payment payment = Payment.builder()
                .workspaceId(subscription.getWorkspaceId())
                .subscriptionId(subscription.getId())
                .stripeInvoiceId("in_" + UUID.randomUUID().toString().substring(0, 14))
                .stripePaymentIntentId("pi_" + UUID.randomUUID().toString().substring(0, 14))
                .amountDue(new BigDecimal("29.99"))
                .amountPaid(status == PaymentStatus.SUCCEEDED ? new BigDecimal("29.99") : BigDecimal.ZERO)
                .currency("USD")
                .status(status)
                .billingPeriodStart(now.minus(30, ChronoUnit.DAYS))
                .billingPeriodEnd(now)
                .description("Subscription payment")
                .build();
        return paymentRepository.save(payment);
    }

    public PaymentSession createPaymentSession(UUID workspaceId, UUID userId, Plan plan,
                                               PaymentSessionStatus status) {
        PaymentSession session = PaymentSession.builder()
                .workspaceId(workspaceId)
                .userId(userId)
                .planId(plan.getId())
                .type(PaymentSessionType.PAYMENT)
                .intent(PaymentSessionIntent.SUBSCRIPTION_CREATE)
                .stripeSessionId("cs_" + UUID.randomUUID().toString().substring(0, 14))
                .stripeUrl("https://checkout.stripe.com/test/" + UUID.randomUUID())
                .status(status)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();
        return paymentSessionRepository.save(session);
    }

    public UsageLog createUsageLog(Subscription subscription, UUID userId, String model,
                                   long tokensIn, long tokensOut, BigDecimal costCredits) {
        UsageLog log = UsageLog.builder()
                .subscriptionId(subscription.getId())
                .workspaceId(subscription.getWorkspaceId())
                .userId(userId)
                .conversationId(UUID.randomUUID())
                .messageId(UUID.randomUUID())
                .sourceService("chat-service")
                .provider("openai")
                .model(model)
                .tokensIn(tokensIn)
                .tokensOut(tokensOut)
                .costCredits(costCredits)
                .occurredAt(Instant.now())
                .build();
        return usageLogRepository.save(log);
    }

    public SubscriptionUsageCounter createUsageCounter(Subscription subscription,
                                                       LocalDate periodStart, LocalDate periodEnd,
                                                       BigDecimal usedValue, BigDecimal reservedBudget) {
        SubscriptionUsageCounter counter = SubscriptionUsageCounter.builder()
                .subscriptionId(subscription.getId())
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .metric(UsageMetric.CREDITS)
                .usedValue(usedValue)
                .reservedBudget(reservedBudget)
                .build();
        return usageCounterRepository.save(counter);
    }

    public void cleanUp() {
        jdbcTemplate.execute("TRUNCATE TABLE usage_logs CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE subscription_usage_counters CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE payment_sessions CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE payments CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE subscriptions CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE llm_models CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE llm_providers CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE plans CASCADE");
    }
}
