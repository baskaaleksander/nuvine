package com.baskaaleksander.nuvine.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "embedding_jobs",
        indexes = {
                @Index(
                        name = "idx_embedding_jobs_workspace_project",
                        columnList = "workspace_id, project_id"
                ),
                @Index(
                        name = "idx_embedding_jobs_document",
                        columnList = "document_id"
                ),
                @Index(
                        name = "idx_embedding_jobs_status",
                        columnList = "status"
                )
        }
)
public class EmbeddingJob {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID workspaceId;
    @Column(nullable = false)
    private UUID projectId;
    @Column(nullable = false)
    private UUID documentId;
    @Column(nullable = false)
    private UUID ingestionJobId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EmbeddingStatus status;

    @Column(nullable = false)
    private int totalChunks;
    @Column(nullable = false)
    private int processedChunks;

    @Column(length = 50)
    private String modelUsed;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

}
