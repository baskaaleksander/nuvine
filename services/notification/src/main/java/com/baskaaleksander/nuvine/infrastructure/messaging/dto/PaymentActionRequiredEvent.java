package com.baskaaleksander.nuvine.infrastructure.messaging.dto;

public record PaymentActionRequiredEvent(
        String ownerEmail,
        String invoiceId,
        String invoiceUrl,
        String workspaceId,
        String workspaceName,
        String workspaceOwnerId
) {
}
