package com.baskaaleksander.nuvine.application.dto;

import com.baskaaleksander.nuvine.domain.model.NotificationType;

public record CreateFailedNotificationRequest(
        String userId,
        NotificationType type,
        String payload,
        String originalTopic,
        String originalPartition,
        String originalOffset,
        String exceptionMessage,
        String exceptionClass
        ) {
}
