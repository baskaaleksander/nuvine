package com.baskaaleksander.nuvine.infrastructure.messaging.dto;

import java.util.UUID;

public record WorkspaceDeletedEvent(UUID workspaceId, String stripeSubscriptionId) {}
