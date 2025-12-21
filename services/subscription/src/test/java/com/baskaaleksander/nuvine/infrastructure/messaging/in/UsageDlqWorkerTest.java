package com.baskaaleksander.nuvine.infrastructure.messaging.in;

import com.baskaaleksander.nuvine.TestFixtures;
import com.baskaaleksander.nuvine.domain.service.UsageService;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DlqMessage;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsageDlqWorker")
class UsageDlqWorkerTest {

    @Mock
    private UsageService usageService;

    @Mock
    private UsageDlqProducer usageDlqProducer;

    @InjectMocks
    private UsageDlqWorker dlqWorker;

    @Captor
    private ArgumentCaptor<DlqMessage> dlqMessageCaptor;

    private static final int MAX_RETRY_ATTEMPTS = 10;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(dlqWorker, "maxRetryAttempts", MAX_RETRY_ATTEMPTS);
    }

    @Test
    @DisplayName("processDlqBatch processes message successfully on retry")
    void processDlqBatch_processesSuccessfully_onRetry() {
        DlqMessage dlqMessage = TestFixtures.dlqMessage();
        List<DlqMessage> messages = List.of(dlqMessage);

        dlqWorker.processDlqBatch(messages);

        verify(usageService).logTokenUsage(dlqMessage.originalEvent());
        verify(usageDlqProducer, never()).sendToDlq(any(DlqMessage.class));
        verify(usageDlqProducer, never()).sendToDeadLetter(any(DlqMessage.class));
    }

    @Test
    @DisplayName("processDlqBatch sends back to DLQ when processing fails and under max retries")
    void processDlqBatch_sendsToDlq_whenFailsUnderMaxRetries() {
        DlqMessage dlqMessage = TestFixtures.dlqMessage();
        List<DlqMessage> messages = List.of(dlqMessage);
        doThrow(new RuntimeException("Processing failed again"))
                .when(usageService).logTokenUsage(dlqMessage.originalEvent());

        dlqWorker.processDlqBatch(messages);

        verify(usageDlqProducer).sendToDlq(dlqMessageCaptor.capture());
        verify(usageDlqProducer, never()).sendToDeadLetter(any(DlqMessage.class));

        DlqMessage capturedMessage = dlqMessageCaptor.getValue();
        assertThat(capturedMessage.attemptCount()).isEqualTo(2);
        assertThat(capturedMessage.errorMessage()).contains("Processing failed again");
    }

    @Test
    @DisplayName("processDlqBatch sends to dead letter when max retries exceeded")
    void processDlqBatch_sendsToDeadLetter_whenMaxRetriesExceeded() {
        DlqMessage dlqMessage = TestFixtures.dlqMessage(9);
        List<DlqMessage> messages = List.of(dlqMessage);
        doThrow(new RuntimeException("Final failure"))
                .when(usageService).logTokenUsage(dlqMessage.originalEvent());

        dlqWorker.processDlqBatch(messages);

        verify(usageDlqProducer, never()).sendToDlq(any(DlqMessage.class));
        verify(usageDlqProducer).sendToDeadLetter(dlqMessageCaptor.capture());

        DlqMessage capturedMessage = dlqMessageCaptor.getValue();
        assertThat(capturedMessage.attemptCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("processDlqBatch processes multiple messages in batch")
    void processDlqBatch_processesMultipleMessages() {
        DlqMessage dlqMessage1 = TestFixtures.dlqMessage();
        DlqMessage dlqMessage2 = TestFixtures.dlqMessage();
        List<DlqMessage> messages = List.of(dlqMessage1, dlqMessage2);

        dlqWorker.processDlqBatch(messages);

        verify(usageService).logTokenUsage(dlqMessage1.originalEvent());
        verify(usageService).logTokenUsage(dlqMessage2.originalEvent());
        verify(usageDlqProducer, never()).sendToDlq(any(DlqMessage.class));
        verify(usageDlqProducer, never()).sendToDeadLetter(any(DlqMessage.class));
    }
}
