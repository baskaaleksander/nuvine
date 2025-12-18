package com.baskaaleksander.nuvine.infrastructure.messaging.in;

import com.baskaaleksander.nuvine.domain.service.UsageService;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DlqMessage;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.LogTokenUsageEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.UsageDlqProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LogTokenUsageEventConsumer {

    private final UsageService usageService;
    private final UsageDlqProducer usageDlqProducer;

    @Value("${topics.log-token-usage-topic}")
    private String logTokenUsageTopic;

    @KafkaListener(topics = "${topics.log-token-usage-topic}")
    public void consumeLogTokenUsageEvent(LogTokenUsageEvent event) {
        log.info("LOG_TOKEN_USAGE EVENT RECEIVED workspaceId={}", event.workspaceId());
        log.debug("LOG_TOKEN_USAGE EVENT RECEIVED event={}", event);

        try {
            usageService.logTokenUsage(event);
            log.info("LOG_TOKEN_USAGE EVENT PROCESSED workspaceId={}", event.workspaceId());
        } catch (Exception e) {
            log.error("LOG_TOKEN_USAGE EVENT FAILED workspaceId={} error={}",
                    event.workspaceId(), e.getMessage(), e);

            DlqMessage dlqMessage = DlqMessage.createInitial(event, e, logTokenUsageTopic);
            usageDlqProducer.sendToDlq(dlqMessage);

            log.info("LOG_TOKEN_USAGE EVENT SENT TO DLQ workspaceId={}", event.workspaceId());
        }
    }
}
