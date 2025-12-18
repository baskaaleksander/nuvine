package com.baskaaleksander.nuvine.infrastructure.messaging.dto;

import java.time.Instant;

public record EmbeddingCompletedDlqMessage(
        EmbeddingCompletedEvent originalEvent,
        int attemptCount,
        String errorMessage,
        String errorClass,
        Instant firstFailedAt,
        Instant lastFailedAt,
        String originalTopic
) {

    public static EmbeddingCompletedDlqMessage createInitial(EmbeddingCompletedEvent event, Exception e, String originalTopic) {
        Instant now = Instant.now();
        return new EmbeddingCompletedDlqMessage(
                event,
                1,
                e.getMessage(),
                e.getClass().getName(),
                now,
                now,
                originalTopic
        );
    }

    public EmbeddingCompletedDlqMessage incrementAttempt(Exception e) {
        return new EmbeddingCompletedDlqMessage(
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
