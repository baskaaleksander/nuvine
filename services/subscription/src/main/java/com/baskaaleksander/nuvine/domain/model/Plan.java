package com.baskaaleksander.nuvine.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Table(
        name = "plans",
        indexes = {
                @Index(name = "idx_plans_code", columnList = "code"),
                @Index(name = "idx_plans_stripe_price_id", columnList = "stripe_price_id"),
                @Index(name = "idx_plans_billing_period", columnList = "billing_period")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_plans_code", columnNames = {"code"})
        }
)
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "stripe_price_id", nullable = false, length = 128)
    private String stripePriceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BillingPeriod billingPeriod;

    @Column(name = "included_credits", nullable = false, length = 64)
    private String includedCredits;

    @Column(name = "max_storage_size", nullable = false)
    private long maxStorageSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "hard_limit_behaviour", nullable = false, length = 32)
    private HardLimitBehaviour hardLimitBehaviour;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @LastModifiedDate
    private Instant updatedAt;

}