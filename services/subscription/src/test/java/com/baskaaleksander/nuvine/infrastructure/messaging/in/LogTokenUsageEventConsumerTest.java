package com.baskaaleksander.nuvine.infrastructure.messaging.in;

import com.baskaaleksander.nuvine.TestFixtures;
import com.baskaaleksander.nuvine.domain.service.UsageService;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DlqMessage;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.LogTokenUsageEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.UsageDlqProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LogTokenUsageEventConsumer")
class LogTokenUsageEventConsumerTest {

    @Mock
    private UsageService usageService;

    @Mock
    private UsageDlqProducer usageDlqProducer;

    @InjectMocks
    private LogTokenUsageEventConsumer consumer;

    @Captor
    private ArgumentCaptor<DlqMessage> dlqMessageCaptor;

    private static final String LOG_TOKEN_USAGE_TOPIC = "log-token-usage-topic";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(consumer, "logTokenUsageTopic", LOG_TOKEN_USAGE_TOPIC);
    }

    @Test
    @DisplayName("consumeLogTokenUsageEvent processes event successfully")
    void consumeLogTokenUsageEvent_processesEventSuccessfully() {
        LogTokenUsageEvent event = TestFixtures.logTokenUsageEvent();

        consumer.consumeLogTokenUsageEvent(event);

        verify(usageService).logTokenUsage(event);
        verify(usageDlqProducer, never()).sendToDlq(any(DlqMessage.class));
    }

    @Test
    @DisplayName("consumeLogTokenUsageEvent sends to DLQ on exception")
    void consumeLogTokenUsageEvent_sendsToDlq_onException() {
        LogTokenUsageEvent event = TestFixtures.logTokenUsageEvent();
        RuntimeException exception = new RuntimeException("Processing failed");
        doThrow(exception).when(usageService).logTokenUsage(event);

        consumer.consumeLogTokenUsageEvent(event);

        verify(usageDlqProducer).sendToDlq(dlqMessageCaptor.capture());

        DlqMessage dlqMessage = dlqMessageCaptor.getValue();
        assertThat(dlqMessage.originalEvent()).isEqualTo(event);
        assertThat(dlqMessage.originalTopic()).isEqualTo(LOG_TOKEN_USAGE_TOPIC);
        assertThat(dlqMessage.attemptCount()).isEqualTo(1);
        assertThat(dlqMessage.errorMessage()).contains("Processing failed");
    }

    @Test
    @DisplayName("consumeLogTokenUsageEvent handles different event payloads")
    void consumeLogTokenUsageEvent_handlesDifferentPayloads() {
        LogTokenUsageEvent event = TestFixtures.logTokenUsageEvent(5000L, 2500L);

        consumer.consumeLogTokenUsageEvent(event);

        verify(usageService).logTokenUsage(event);
        verify(usageDlqProducer, never()).sendToDlq(any(DlqMessage.class));
    }
}
