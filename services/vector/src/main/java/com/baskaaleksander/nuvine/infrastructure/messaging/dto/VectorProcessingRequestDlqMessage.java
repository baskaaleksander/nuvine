package com.baskaaleksander.nuvine.infrastructure.messaging.dto;

import java.time.Instant;

public record VectorProcessingRequestDlqMessage(
        VectorProcessingRequestEvent originalEvent,
        int attemptCount,
        String errorMessage,
        String errorClass,
        Instant firstFailedAt,
        Instant lastFailedAt,
        String originalTopic
) {

    public static VectorProcessingRequestDlqMessage createInitial(VectorProcessingRequestEvent event, Exception e, String originalTopic) {
        Instant now = Instant.now();
        return new VectorProcessingRequestDlqMessage(
                event,
                1,
                e.getMessage(),
                e.getClass().getName(),
                now,
                now,
                originalTopic
        );
    }

    public VectorProcessingRequestDlqMessage incrementAttempt(Exception e) {
        return new VectorProcessingRequestDlqMessage(
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
