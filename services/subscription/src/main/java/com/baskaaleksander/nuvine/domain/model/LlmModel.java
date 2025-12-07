package com.baskaaleksander.nuvine.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "llm_models",
        uniqueConstraints = @UniqueConstraint(columnNames = {"provider_id", "model_key"}))
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LlmModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private LlmProvider provider;

    @Column(nullable = false)
    private String modelKey;

    private String displayName;

    @Embedded
    private ModelPricing pricing;

    @Column(nullable = false)
    private Boolean free = false;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private Instant effectiveFrom;

    private Instant effectiveTo;

}

