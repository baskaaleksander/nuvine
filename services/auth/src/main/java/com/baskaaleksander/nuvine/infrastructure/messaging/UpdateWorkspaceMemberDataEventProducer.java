package com.baskaaleksander.nuvine.infrastructure.messaging;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.UpdateWorkspaceMemberDataEvent;
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
public class UpdateWorkspaceMemberDataEventProducer {

    private final KafkaTemplate<String, UpdateWorkspaceMemberDataEvent> kafkaTemplate;

    @Value("${topics.update-workspace-member-data-topic}")
    private String topic;

    public void sendUpdateWorkspaceMemberDateEvent(UpdateWorkspaceMemberDataEvent event) {
        Message<UpdateWorkspaceMemberDataEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .build();

        kafkaTemplate.send(message);

        log.info("UPDATE_WORKSPACE_MEMBER_DATA EVENT SUCCESS email={}", event.userId());

    }
}
