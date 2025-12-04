package com.baskaaleksander.nuvine.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "usage_logs",
        indexes = {
                @Index(name = "idx_usage_logs_subscription_period", columnList = "subscription_id, occurred_at"),
                @Index(name = "idx_usage_logs_workspace_period", columnList = "workspace_id, occurred_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "conversation_id")
    private UUID conversationId;

    @Column(name = "message_id")
    private UUID messageId;

    @Column(name = "source_service", nullable = false, length = 64)
    private String sourceService;

    @Column(name = "provider", length = 64)
    private String provider;

    @Column(name = "model", nullable = false, length = 128)
    private String model;

    @Column(name = "tokens_in", nullable = false)
    private long tokensIn;

    @Column(name = "tokens_out", nullable = false)
    private long tokensOut;

    @Column(name = "cost_credits", nullable = false, precision = 18, scale = 6)
    private BigDecimal costCredits;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}
