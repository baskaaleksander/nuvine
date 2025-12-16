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
        name = "workspace_member_invite_tokens",
        indexes = {
                @Index(name = "idx_workspace_member_invite_tokens_workspace_member_id", columnList = "workspace_member_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_workspace_member_invite_tokens_token", columnNames = {"token"})
        }
)
public class WorkspaceMemberInviteToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 2048)
    private String token;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant usedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_member_id", nullable = false)
    private WorkspaceMember workspaceMember;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Version
    private Long version;
}
