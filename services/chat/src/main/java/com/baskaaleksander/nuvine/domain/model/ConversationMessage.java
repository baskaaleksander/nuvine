package com.baskaaleksander.nuvine.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(
        name = "conversation_message",
        indexes = {
                @Index(
                        name = "idx_conversation_message_conversation_created_at",
                        columnList = "conversation_id, created_at"
                ),
                @Index(
                        name = "idx_conversation_message_created_at",
                        columnList = "created_at"
                )
        }
)
public class ConversationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ConversationRole role;

    @Column(name = "model_used", nullable = false, length = 100)
    private String modelUsed;

    @Column(name = "tokens_cost")
    private int tokensCost;

    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false, name = "project_id")
    private UUID projectId;

    @Column(nullable = false, name = "workspace_id")
    private UUID workspaceId;

    @Column(nullable = false)
    private double cost;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}