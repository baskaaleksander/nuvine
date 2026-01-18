package com.baskaaleksander.nuvine.integration.kafka;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DlqMessage;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.LogTokenUsageEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.PaymentActionRequiredEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.PaymentActionRequiredEventProducer;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.UsageDlqProducer;
import com.baskaaleksander.nuvine.integration.base.BaseKafkaIntegrationTest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaProducersIT extends BaseKafkaIntegrationTest {

    @Autowired
    private PaymentActionRequiredEventProducer paymentActionRequiredEventProducer;

    @Autowired
    private UsageDlqProducer usageDlqProducer;

    @Value("${topics.payment-action-required-topic}")
    private String paymentActionRequiredTopic;

    @Value("${topics.usage-logs-dlq-topic}")
    private String usageLogsDlqTopic;

    @Test
    @DisplayName("Should publish PaymentActionRequiredEvent correctly")
    void shouldPublishPaymentActionRequiredEvent() throws InterruptedException {
        BlockingQueue<ConsumerRecord<String, PaymentActionRequiredEvent>> records =
                createConsumer(paymentActionRequiredTopic, PaymentActionRequiredEvent.class);

        UUID workspaceId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        PaymentActionRequiredEvent event = new PaymentActionRequiredEvent(
                "owner@example.com",
                "in_test123",
                "https://invoice.stripe.com/test",
                workspaceId.toString(),
                "Test Workspace",
                ownerId.toString()
        );

        paymentActionRequiredEventProducer.producePaymentActionRequiredEvent(event);

        PaymentActionRequiredEvent receivedEvent = awaitMessage(records, 10, TimeUnit.SECONDS);
        assertThat(receivedEvent).isNotNull();
        assertThat(receivedEvent.ownerEmail()).isEqualTo("owner@example.com");
        assertThat(receivedEvent.invoiceId()).isEqualTo("in_test123");
        assertThat(receivedEvent.workspaceId()).isEqualTo(workspaceId.toString());
    }

    @Test
    @DisplayName("Should publish DlqMessage to usage logs DLQ")
    void shouldPublishDlqMessage() throws InterruptedException {
        BlockingQueue<ConsumerRecord<String, DlqMessage>> records =
                createConsumer(usageLogsDlqTopic, DlqMessage.class);

        LogTokenUsageEvent originalEvent = new LogTokenUsageEvent(
                UUID.randomUUID().toString(),
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

        DlqMessage dlqMessage = DlqMessage.createInitial(
                originalEvent,
                new RuntimeException("Test error"),
                "log-token-usage-topic-test"
        );

        usageDlqProducer.sendToDlq(dlqMessage);

        DlqMessage receivedMessage = awaitMessage(records, 10, TimeUnit.SECONDS);
        assertThat(receivedMessage).isNotNull();
        assertThat(receivedMessage.originalEvent().model()).isEqualTo("gpt-4");
        assertThat(receivedMessage.attemptCount()).isEqualTo(1);
        assertThat(receivedMessage.errorMessage()).isEqualTo("Test error");
    }
}
