package com.baskaaleksander.nuvine.application.dto;

import com.baskaaleksander.nuvine.domain.model.NotificationType;

public record CreateNotificationRequest(
        String userId,
        NotificationType type,
        String payload
) {
}
