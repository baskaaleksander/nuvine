package com.baskaaleksander.nuvine.integration.kafka;

import com.baskaaleksander.nuvine.domain.model.*;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DlqMessage;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.LogTokenUsageEvent;
import com.baskaaleksander.nuvine.infrastructure.persistence.UsageLogRepository;
import com.baskaaleksander.nuvine.integration.base.BaseKafkaIntegrationTest;
import com.baskaaleksander.nuvine.integration.support.TestDataBuilder;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class KafkaConsumersIT extends BaseKafkaIntegrationTest {

    @Autowired
    private TestDataBuilder testDataBuilder;

    @Autowired
    private UsageLogRepository usageLogRepository;

    @Value("${topics.log-token-usage-topic}")
    private String logTokenUsageTopic;

    @Value("${topics.usage-logs-dlq-topic}")
    private String usageLogsDlqTopic;

    private UUID workspaceId;
    private Subscription subscription;

    @BeforeEach
    void setUp() {
        testDataBuilder.cleanUp();
        workspaceId = UUID.randomUUID();
        Plan plan = testDataBuilder.createProPlan();
        subscription = testDataBuilder.createActiveSubscription(workspaceId, plan);

        LlmProvider provider = testDataBuilder.createLlmProvider("openai", "OpenAI");
        testDataBuilder.createLlmModel(provider, "gpt-4", "GPT-4");
    }

    @Test
    @DisplayName("Should create UsageLog entry from LogTokenUsageEvent")
    void shouldCreateUsageLogFromEvent() throws InterruptedException {
        ensureTopicExists(logTokenUsageTopic);

        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        LogTokenUsageEvent event = new LogTokenUsageEvent(
                workspaceId.toString(),
                userId.toString(),
                conversationId.toString(),
                messageId.toString(),
                "gpt-4",
                "openai",
                "chat-service",
                100L,
                200L,
                Instant.now()
        );

        kafkaTemplate.send(logTokenUsageTopic, workspaceId.toString(), event);
        kafkaTemplate.flush();

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            List<UsageLog> logs = usageLogRepository.findAll();
            assertThat(logs).isNotEmpty();

            UsageLog log = logs.stream()
                    .filter(l -> l.getWorkspaceId().equals(workspaceId))
                    .findFirst()
                    .orElse(null);

            assertThat(log).isNotNull();
            assertThat(log.getModel()).isEqualTo("gpt-4");
            assertThat(log.getTokensIn()).isEqualTo(100L);
            assertThat(log.getTokensOut()).isEqualTo(200L);
        });
    }

    @Test
    @DisplayName("Should send to DLQ when subscription not found")
    void shouldSendToDlqWhenSubscriptionNotFound() throws InterruptedException {
        ensureTopicExists(logTokenUsageTopic);

        BlockingQueue<ConsumerRecord<String, DlqMessage>> dlqRecords =
                createConsumer(usageLogsDlqTopic, DlqMessage.class);

        UUID nonExistentWorkspaceId = UUID.randomUUID();
        LogTokenUsageEvent event = new LogTokenUsageEvent(
                nonExistentWorkspaceId.toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "gpt-4",
                "openai",
                "chat-service",
                100L,
                200L,
                Instant.now()
        );

        kafkaTemplate.send(logTokenUsageTopic, nonExistentWorkspaceId.toString(), event);
        kafkaTemplate.flush();

        DlqMessage dlqMessage = awaitMessage(dlqRecords, 15, TimeUnit.SECONDS);
        assertThat(dlqMessage).isNotNull();
        assertThat(dlqMessage.originalEvent().workspaceId()).isEqualTo(nonExistentWorkspaceId.toString());
        assertThat(dlqMessage.attemptCount()).isEqualTo(1);
    }
}
