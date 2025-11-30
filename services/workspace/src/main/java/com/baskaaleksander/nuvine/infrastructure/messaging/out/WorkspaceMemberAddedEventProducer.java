package com.baskaaleksander.nuvine.infrastructure.messaging.out;

import com.baskaaleksander.nuvine.application.util.MaskingUtil;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.WorkspaceMemberAddedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class WorkspaceMemberAddedEventProducer {

    @Value("${topics.workspace-member-added-topic}")
    private String topic;

    private final KafkaTemplate<String, WorkspaceMemberAddedEvent> kafkaTemplate;

    public void sendWorkspaceMemberAddedEvent(WorkspaceMemberAddedEvent event) {
        Message<WorkspaceMemberAddedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .build();

        kafkaTemplate.send(message);
        log.info("WORKSPACE_MEMBER_ADDED EVENT SUCCESS email={}", MaskingUtil.maskEmail(event.email()));
    }
}
