package com.baskaaleksander.nuvine.infrastructure.messaging.dto;

import java.time.Instant;

public record VectorProcessingCompletedDlqMessage(
        VectorProcessingCompletedEvent originalEvent,
        int attemptCount,
        String errorMessage,
        String errorClass,
        Instant firstFailedAt,
        Instant lastFailedAt,
        String originalTopic
) {

    public static VectorProcessingCompletedDlqMessage createInitial(VectorProcessingCompletedEvent event, Exception e, String originalTopic) {
        Instant now = Instant.now();
        return new VectorProcessingCompletedDlqMessage(
                event,
                1,
                e.getMessage(),
                e.getClass().getName(),
                now,
                now,
                originalTopic
        );
    }

    public VectorProcessingCompletedDlqMessage incrementAttempt(Exception e) {
        return new VectorProcessingCompletedDlqMessage(
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
