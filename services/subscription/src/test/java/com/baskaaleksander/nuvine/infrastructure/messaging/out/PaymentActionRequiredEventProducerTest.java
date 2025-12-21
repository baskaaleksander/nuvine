package com.baskaaleksander.nuvine.infrastructure.messaging.out;

import com.baskaaleksander.nuvine.TestFixtures;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.PaymentActionRequiredEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentActionRequiredEventProducer")
class PaymentActionRequiredEventProducerTest {

    @Mock
    private KafkaTemplate<String, PaymentActionRequiredEvent> kafkaTemplate;

    @InjectMocks
    private PaymentActionRequiredEventProducer producer;

    @Captor
    private ArgumentCaptor<Message<PaymentActionRequiredEvent>> messageCaptor;

    private static final String TOPIC = "payment-action-required-topic";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(producer, "topic", TOPIC);
    }

    @Test
    @DisplayName("producePaymentActionRequiredEvent sends message to correct topic")
    void producePaymentActionRequiredEvent_sendsToCorrectTopic() {
        PaymentActionRequiredEvent event = TestFixtures.paymentActionRequiredEvent();

        producer.producePaymentActionRequiredEvent(event);

        verify(kafkaTemplate).send(messageCaptor.capture());

        Message<PaymentActionRequiredEvent> capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.getPayload()).isEqualTo(event);
        assertThat(capturedMessage.getHeaders().get(KafkaHeaders.TOPIC)).isEqualTo(TOPIC);
    }

    @Test
    @DisplayName("producePaymentActionRequiredEvent includes all event fields in payload")
    void producePaymentActionRequiredEvent_includesAllEventFields() {
        PaymentActionRequiredEvent event = TestFixtures.paymentActionRequiredEvent();

        producer.producePaymentActionRequiredEvent(event);

        verify(kafkaTemplate).send(messageCaptor.capture());

        PaymentActionRequiredEvent payload = messageCaptor.getValue().getPayload();
        assertThat(payload.ownerEmail()).isEqualTo(event.ownerEmail());
        assertThat(payload.invoiceId()).isEqualTo(event.invoiceId());
        assertThat(payload.invoiceUrl()).isEqualTo(event.invoiceUrl());
        assertThat(payload.workspaceId()).isEqualTo(event.workspaceId());
        assertThat(payload.workspaceName()).isEqualTo(event.workspaceName());
        assertThat(payload.workspaceOwnerId()).isEqualTo(event.workspaceOwnerId());
    }
}
