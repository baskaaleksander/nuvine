package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.domain.model.UsageLog;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.LogTokenUsageEvent;
import com.baskaaleksander.nuvine.infrastructure.persistence.SubscriptionUsageCounterRepository;
import com.baskaaleksander.nuvine.infrastructure.persistence.UsageLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UsageService {

    private final UsageLogRepository usageLogRepository;
    private final SubscriptionUsageCounterRepository subscriptionUsageCounterRepository;

    public void logTokenUsage(LogTokenUsageEvent event) {
        UsageLog usageLog = UsageLog.builder()
                .workspaceId(UUID.fromString(event.workspaceId()))
                .userId(UUID.fromString(event.userId()))
                .conversationId(UUID.fromString(event.conversationId()))
                .messageId(UUID.fromString(event.messageId()))
                .model(event.model())
                .provider(event.provider())
                .sourceService(event.sourceService())
                .tokensIn(event.tokensIn())
                .tokensOut(event.tokensOut())
                .occurredAt(event.occurredAt())
                .build();
        usageLogRepository.save(usageLog);
    }

}
