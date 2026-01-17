package com.baskaaleksander.nuvine.integration.kafka;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DocumentIngestionCompletedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DocumentIngestionDlqMessage;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.UpdateWorkspaceMemberDataEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.WorkspaceMemberDataUpdateDlqMessage;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.DocumentIngestionDlqProducer;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.WorkspaceMemberDataUpdateDlqProducer;
import com.baskaaleksander.nuvine.integration.base.BaseKafkaIntegrationTest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class KafkaDlqProducersIT extends BaseKafkaIntegrationTest {

    @Autowired
    private DocumentIngestionDlqProducer documentIngestionDlqProducer;

    @Autowired
    private WorkspaceMemberDataUpdateDlqProducer workspaceMemberDataUpdateDlqProducer;

    @Test
    void documentIngestionDlqProducer_sendsToDlq() throws Exception {
        BlockingQueue<ConsumerRecord<String, DocumentIngestionDlqMessage>> queue =
                createConsumer("document-ingestion-dlq-test", DocumentIngestionDlqMessage.class);

        DocumentIngestionCompletedEvent event = new DocumentIngestionCompletedEvent(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
        );
        DocumentIngestionDlqMessage message = new DocumentIngestionDlqMessage(
                event,
                1,
                "boom",
                RuntimeException.class.getName(),
                Instant.now(),
                Instant.now(),
                "document-ingestion-completed-topic-test"
        );

        documentIngestionDlqProducer.sendToDlq(message);

        DocumentIngestionDlqMessage received = awaitMessage(queue, 10, TimeUnit.SECONDS);
        assertNotNull(received);
        assertEquals(event.documentId(), received.originalEvent().documentId());
        assertEquals("document-ingestion-completed-topic-test", received.originalTopic());
    }

    @Test
    void workspaceMemberDataUpdateDlqProducer_sendsToDlq() throws Exception {
        BlockingQueue<ConsumerRecord<String, WorkspaceMemberDataUpdateDlqMessage>> queue =
                createConsumer("workspace-member-data-update-dlq-test", WorkspaceMemberDataUpdateDlqMessage.class);

        UpdateWorkspaceMemberDataEvent event = new UpdateWorkspaceMemberDataEvent(
                UUID.randomUUID().toString(),
                "First",
                "Last",
                "new@example.com"
        );
        WorkspaceMemberDataUpdateDlqMessage message = new WorkspaceMemberDataUpdateDlqMessage(
                event,
                1,
                "boom",
                RuntimeException.class.getName(),
                Instant.now(),
                Instant.now(),
                "update-workspace-member-data-topic-test"
        );

        workspaceMemberDataUpdateDlqProducer.sendToDlq(message);

        WorkspaceMemberDataUpdateDlqMessage received = awaitMessage(queue, 10, TimeUnit.SECONDS);
        assertNotNull(received);
        assertEquals(event.userId(), received.originalEvent().userId());
        assertEquals("update-workspace-member-data-topic-test", received.originalTopic());
    }
}
