package com.baskaaleksander.nuvine.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "ingestion_jobs")
@EntityListeners(AuditingEntityListener.class)
public class IngestionJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID documentId;

    @Column(nullable = false)
    private UUID workspaceId;

    @Column(nullable = false)
    private UUID projectId;

    @Column(nullable = false)
    private String storageKey;

    @Column(nullable = false)
    private String mimeType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private IngestionStatus status;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private IngestionStage stage;

    @Column(nullable = false)
    private int retryCount;

    @Column
    private String lastError;

    @Column
    private UUID createdBy;

    @Column
    @CreatedDate
    private Instant createdAt;

    @Column
    @LastModifiedDate
    private Instant updatedAt;

    @Version
    @Column
    private Long version;
}
