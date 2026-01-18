package com.baskaaleksander.nuvine.infrastructure.messaging.out;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.PaymentActionRequiredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentActionRequiredEventProducer {

    private final KafkaTemplate<String, PaymentActionRequiredEvent> kafkaTemplate;

    @Value("${topics.payment-action-required-topic}")
    private String topic;

    public void producePaymentActionRequiredEvent(PaymentActionRequiredEvent event) {
        Message<PaymentActionRequiredEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .build();
        
        kafkaTemplate.send(message);
    }
}
