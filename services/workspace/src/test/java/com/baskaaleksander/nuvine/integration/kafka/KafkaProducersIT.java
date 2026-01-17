package com.baskaaleksander.nuvine.integration.kafka;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DocumentUploadedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.WorkspaceDeletedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.WorkspaceMemberAddedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.WorkspaceMemberInvitedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.DocumentUploadedEventProducer;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.WorkspaceDeletedEventProducer;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.WorkspaceMemberAddedEventProducer;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.WorkspaceMemberInvitedEventProducer;
import com.baskaaleksander.nuvine.integration.base.BaseKafkaIntegrationTest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class KafkaProducersIT extends BaseKafkaIntegrationTest {

    @Autowired
    private DocumentUploadedEventProducer documentUploadedEventProducer;

    @Autowired
    private WorkspaceMemberAddedEventProducer workspaceMemberAddedEventProducer;

    @Autowired
    private WorkspaceMemberInvitedEventProducer workspaceMemberInvitedEventProducer;

    @Autowired
    private WorkspaceDeletedEventProducer workspaceDeletedEventProducer;

    @Test
    void documentUploadedEvent_isPublished() throws Exception {
        BlockingQueue<ConsumerRecord<String, DocumentUploadedEvent>> queue =
                createConsumer("document-uploaded-topic-test", DocumentUploadedEvent.class);

        DocumentUploadedEvent event = new DocumentUploadedEvent(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "documents/file.pdf",
                "application/pdf",
                1024L
        );

        documentUploadedEventProducer.sendDocumentUploadedEvent(event);

        DocumentUploadedEvent received = awaitMessage(queue, 10, TimeUnit.SECONDS);
        assertNotNull(received);
        assertEquals(event.documentId(), received.documentId());
        assertEquals(event.workspaceId(), received.workspaceId());
        assertEquals(event.projectId(), received.projectId());
    }

    @Test
    void workspaceMemberAddedEvent_isPublished() throws Exception {
        BlockingQueue<ConsumerRecord<String, WorkspaceMemberAddedEvent>> queue =
                createConsumer("workspace-member-added-topic-test", WorkspaceMemberAddedEvent.class);

        WorkspaceMemberAddedEvent event = new WorkspaceMemberAddedEvent(
                "user@example.com",
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "MODERATOR"
        );

        workspaceMemberAddedEventProducer.sendWorkspaceMemberAddedEvent(event);

        WorkspaceMemberAddedEvent received = awaitMessage(queue, 10, TimeUnit.SECONDS);
        assertNotNull(received);
        assertEquals(event.email(), received.email());
        assertEquals(event.workspaceId(), received.workspaceId());
        assertEquals(event.role(), received.role());
    }

    @Test
    void workspaceMemberInvitedEvent_isPublished() throws Exception {
        BlockingQueue<ConsumerRecord<String, WorkspaceMemberInvitedEvent>> queue =
                createConsumer("workspace-member-invited-topic-test", WorkspaceMemberInvitedEvent.class);

        WorkspaceMemberInvitedEvent event = new WorkspaceMemberInvitedEvent(
                "invitee@example.com",
                UUID.randomUUID().toString(),
                "Workspace",
                "VIEWER",
                "invite-token"
        );

        workspaceMemberInvitedEventProducer.sendWorkspaceMemberInvitedEvent(event);

        WorkspaceMemberInvitedEvent received = awaitMessage(queue, 10, TimeUnit.SECONDS);
        assertNotNull(received);
        assertEquals(event.email(), received.email());
        assertEquals(event.token(), received.token());
    }

    @Test
    void workspaceDeletedEvent_isPublished() throws Exception {
        BlockingQueue<ConsumerRecord<String, WorkspaceDeletedEvent>> queue =
                createConsumer("workspace-deleted-test", WorkspaceDeletedEvent.class);

        WorkspaceDeletedEvent event = new WorkspaceDeletedEvent(UUID.randomUUID(), "sub_123");

        workspaceDeletedEventProducer.send(event);

        WorkspaceDeletedEvent received = awaitMessage(queue, 10, TimeUnit.SECONDS);
        assertNotNull(received);
        assertEquals(event.workspaceId(), received.workspaceId());
        assertEquals(event.stripeSubscriptionId(), received.stripeSubscriptionId());
    }
}
