package com.baskaaleksander.subscription.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(
        name = "subscriptions",
        indexes = {
                @Index(name = "idx_subscriptions_workspace_id", columnList = "workspace_id"),
                @Index(name = "idx_subscriptions_plan_id", columnList = "plan_id"),
                @Index(name = "idx_subscriptions_status", columnList = "status"),
                @Index(name = "idx_subscriptions_stripe_customer_id", columnList = "stripe_customer_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_subscriptions_stripe_subscription_id",
                        columnNames = {"stripe_subscription_id"}
                )
        }
)
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "stripe_customer_id", nullable = false, length = 128)
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id", nullable = false, length = 128)
    private String stripeSubscriptionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SubscriptionStatus status;

    @Column(name = "current_period_start")
    private Long currentPeriodStart;

    @Column(name = "current_period_end")
    private Long currentPeriodEnd;

    @Column(name = "cancel_at_period_end", nullable = false)
    private Boolean cancelAtPeriodEnd;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @LastModifiedDate
    private Instant updatedAt;
}
