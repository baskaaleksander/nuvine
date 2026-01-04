package com.baskaaleksander.nuvine.infrastructure.messaging.dto;

import java.time.Instant;

public record WorkspaceMemberDataUpdateDlqMessage(
        UpdateWorkspaceMemberDataEvent originalEvent,
        int attemptCount,
        String errorMessage,
        String errorClass,
        Instant firstFailedAt,
        Instant lastFailedAt,
        String originalTopic
) {

    public static WorkspaceMemberDataUpdateDlqMessage createInitial(
            UpdateWorkspaceMemberDataEvent event, Exception e, String originalTopic) {
        Instant now = Instant.now();
        return new WorkspaceMemberDataUpdateDlqMessage(
                event,
                1,
                e.getMessage(),
                e.getClass().getName(),
                now,
                now,
                originalTopic
        );
    }

    public WorkspaceMemberDataUpdateDlqMessage incrementAttempt(Exception e) {
        return new WorkspaceMemberDataUpdateDlqMessage(
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
