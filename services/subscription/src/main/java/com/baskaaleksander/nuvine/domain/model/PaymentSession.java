package com.baskaaleksander.nuvine.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "payment_sessions",
        indexes = {
                @Index(name = "idx_payment_sessions_workspace", columnList = "workspace_id"),
                @Index(name = "idx_payment_sessions_stripe_session", columnList = "stripe_session_id", unique = true),
                @Index(name = "idx_payment_sessions_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private PaymentSessionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "intent", nullable = false, length = 64)
    private PaymentSessionIntent intent;

    @Column(name = "stripe_session_id", nullable = false, length = 128, unique = true)
    private String stripeSessionId;

    @Column(name = "stripe_url", nullable = false, length = 2048)
    private String stripeUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PaymentSessionStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private String metadataJson;
}
