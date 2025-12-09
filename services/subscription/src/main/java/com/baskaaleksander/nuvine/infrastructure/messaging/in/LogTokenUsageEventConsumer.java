package com.baskaaleksander.nuvine.infrastructure.messaging.in;

import com.baskaaleksander.nuvine.domain.service.UsageService;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.LogTokenUsageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LogTokenUsageEventConsumer {

    private final UsageService usageService;

    @KafkaListener(topics = "${topics.log-token-usage-topic}")
    public void consumeLogTokenUsageEvent(LogTokenUsageEvent event) {
        log.info("LOG_TOKEN_USAGE EVENT RECEIVED workspaceId={}", event.workspaceId());
        log.info("LOG_TOKEN_USAGE EVENT RECEIVED event={}", event);
        usageService.logTokenUsage(event);
        log.info("LOG_TOKEN_USAGE EVENT PROCESSED workspaceId={}", event.workspaceId());
    }
}
