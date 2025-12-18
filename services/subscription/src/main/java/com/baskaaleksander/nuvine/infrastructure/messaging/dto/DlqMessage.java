package com.baskaaleksander.nuvine.infrastructure.messaging.dto;

import java.time.Instant;

public record DlqMessage(
        LogTokenUsageEvent originalEvent,
        int attemptCount,
        String errorMessage,
        String errorClass,
        Instant firstFailedAt,
        Instant lastFailedAt,
        String originalTopic
) {

    public static DlqMessage createInitial(LogTokenUsageEvent event, Exception e, String originalTopic) {
        Instant now = Instant.now();
        return new DlqMessage(
                event,
                1,
                e.getMessage(),
                e.getClass().getName(),
                now,
                now,
                originalTopic
        );
    }

    public DlqMessage incrementAttempt(Exception e) {
        return new DlqMessage(
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
