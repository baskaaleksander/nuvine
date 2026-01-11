package com.baskaaleksander.nuvine.infrastructure.messaging.dto;

import java.time.Instant;

public record WorkspaceDeletedDlqMessage(
        WorkspaceDeletedEvent originalEvent,
        int attemptCount,
        String errorMessage,
        String errorClass,
        Instant firstFailedAt,
        Instant lastFailedAt,
        String originalTopic
) {

    public static WorkspaceDeletedDlqMessage createInitial(WorkspaceDeletedEvent event, Exception e, String originalTopic) {
        Instant now = Instant.now();
        return new WorkspaceDeletedDlqMessage(
                event,
                1,
                e.getMessage(),
                e.getClass().getName(),
                now,
                now,
                originalTopic
        );
    }

    public WorkspaceDeletedDlqMessage incrementAttempt(Exception e) {
        return new WorkspaceDeletedDlqMessage(
                this.originalEvent,
                this.attemptCount + 1,
                e.getMessage(),
                e.getClass().getName(),
                this.firstFailedAt,
                Instant.now(),
                this.originalTopic
        );
    }
}
