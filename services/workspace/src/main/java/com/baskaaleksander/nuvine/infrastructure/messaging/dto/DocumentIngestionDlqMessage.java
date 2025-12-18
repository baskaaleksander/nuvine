package com.baskaaleksander.nuvine.infrastructure.messaging.dto;

import java.time.Instant;

public record DocumentIngestionDlqMessage(
        DocumentIngestionCompletedEvent originalEvent,
        int attemptCount,
        String errorMessage,
        String errorClass,
        Instant firstFailedAt,
        Instant lastFailedAt,
        String originalTopic
) {

    public static DocumentIngestionDlqMessage createInitial(DocumentIngestionCompletedEvent event, Exception e, String originalTopic) {
        Instant now = Instant.now();
        return new DocumentIngestionDlqMessage(
                event,
                1,
                e.getMessage(),
                e.getClass().getName(),
                now,
                now,
                originalTopic
        );
    }

    public DocumentIngestionDlqMessage incrementAttempt(Exception e) {
        return new DocumentIngestionDlqMessage(
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
