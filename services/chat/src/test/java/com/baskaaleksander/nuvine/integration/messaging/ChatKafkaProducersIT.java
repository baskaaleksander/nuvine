package com.baskaaleksander.nuvine.integration.messaging;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.LogTokenUsageEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.LogTokenUsageEventProducer;
import com.baskaaleksander.nuvine.integration.base.BaseKafkaIntegrationTest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ChatKafkaProducersIT extends BaseKafkaIntegrationTest {

    private static final String LOG_TOKEN_USAGE_TOPIC = "log-token-usage-topic";

    @Autowired
    private LogTokenUsageEventProducer logTokenUsageEventProducer;

    @Test
    @DisplayName("LogTokenUsageEvent is published with all fields")
    void logTokenUsageEvent_isPublished_withAllFields() throws InterruptedException {
        // given
        BlockingQueue<ConsumerRecord<String, LogTokenUsageEvent>> records =
                createConsumer(LOG_TOKEN_USAGE_TOPIC, LogTokenUsageEvent.class);

        String workspaceId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();
        String conversationId = UUID.randomUUID().toString();
        String messageId = UUID.randomUUID().toString();
        String model = "gpt-4";
        String provider = "openai";
        String sourceService = "chat-service";
        long tokensIn = 150;
        long tokensOut = 200;
        Instant occurredAt = Instant.now();

        LogTokenUsageEvent event = new LogTokenUsageEvent(
                workspaceId,
                userId,
                conversationId,
                messageId,
                model,
                provider,
                sourceService,
                tokensIn,
                tokensOut,
                occurredAt
        );

        // when
        logTokenUsageEventProducer.produceLogTokenUsageEvent(event);

        // then
        LogTokenUsageEvent receivedEvent = awaitMessage(records, 10, TimeUnit.SECONDS);

        assertThat(receivedEvent).isNotNull();
        assertThat(receivedEvent.workspaceId()).isEqualTo(workspaceId);
        assertThat(receivedEvent.userId()).isEqualTo(userId);
        assertThat(receivedEvent.conversationId()).isEqualTo(conversationId);
        assertThat(receivedEvent.messageId()).isEqualTo(messageId);
        assertThat(receivedEvent.model()).isEqualTo(model);
        assertThat(receivedEvent.provider()).isEqualTo(provider);
        assertThat(receivedEvent.sourceService()).isEqualTo(sourceService);
        assertThat(receivedEvent.tokensIn()).isEqualTo(tokensIn);
        assertThat(receivedEvent.tokensOut()).isEqualTo(tokensOut);
        assertThat(receivedEvent.occurredAt()).isEqualTo(occurredAt);
    }

    @Test
    @DisplayName("LogTokenUsageEvent contains correct workspaceId, userId, model, and tokens")
    void logTokenUsageEvent_containsCorrectFields() throws InterruptedException {
        // given
        BlockingQueue<ConsumerRecord<String, LogTokenUsageEvent>> records =
                createConsumer(LOG_TOKEN_USAGE_TOPIC, LogTokenUsageEvent.class);

        String workspaceId = "workspace-123";
        String userId = "user-456";
        String model = "claude-3-opus";
        String provider = "anthropic";
        long tokensIn = 500;
        long tokensOut = 1000;

        LogTokenUsageEvent event = new LogTokenUsageEvent(
                workspaceId,
                userId,
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                model,
                provider,
                "chat-service",
                tokensIn,
                tokensOut,
                Instant.now()
        );

        // when
        logTokenUsageEventProducer.produceLogTokenUsageEvent(event);

        // then
        LogTokenUsageEvent receivedEvent = awaitMessage(records, 10, TimeUnit.SECONDS);

        assertThat(receivedEvent).isNotNull();
        assertThat(receivedEvent.workspaceId()).isEqualTo(workspaceId);
        assertThat(receivedEvent.userId()).isEqualTo(userId);
        assertThat(receivedEvent.model()).isEqualTo(model);
        assertThat(receivedEvent.provider()).isEqualTo(provider);
        assertThat(receivedEvent.tokensIn()).isEqualTo(tokensIn);
        assertThat(receivedEvent.tokensOut()).isEqualTo(tokensOut);
    }
}
