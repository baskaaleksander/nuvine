package com.baskaaleksander.nuvine.integration.kafka;

import com.baskaaleksander.nuvine.domain.model.Document;
import com.baskaaleksander.nuvine.domain.model.DocumentStatus;
import com.baskaaleksander.nuvine.domain.model.Workspace;
import com.baskaaleksander.nuvine.domain.model.WorkspaceMember;
import com.baskaaleksander.nuvine.domain.model.WorkspaceMemberStatus;
import com.baskaaleksander.nuvine.domain.model.WorkspaceRole;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DocumentIngestionCompletedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DocumentIngestionDlqMessage;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.UpdateWorkspaceMemberDataEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.WorkspaceMemberDataUpdateDlqMessage;
import com.baskaaleksander.nuvine.integration.base.BaseKafkaIntegrationTest;
import com.baskaaleksander.nuvine.integration.support.TestDataBuilder;
import com.baskaaleksander.nuvine.infrastructure.repository.DocumentRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceMemberRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class KafkaConsumersIT extends BaseKafkaIntegrationTest {

    @Autowired
    private TestDataBuilder testData;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @BeforeEach
    void setUp() {
        testData.cleanUp();
    }

    @Test
    void documentIngestionCompleted_updatesDocumentStatus() {
        Workspace workspace = testData.createWorkspace("Workspace", UUID.randomUUID(), "user@example.com");
        var project = testData.createProject(workspace.getId(), "Project");
        Document document = testData.createDocument(workspace.getId(), project.getId(), UUID.randomUUID(), "Doc", DocumentStatus.UPLOADED);

        DocumentIngestionCompletedEvent event = new DocumentIngestionCompletedEvent(
                document.getId().toString(),
                workspace.getId().toString(),
                project.getId().toString()
        );

        kafkaTemplate.send("document-ingestion-completed-topic-test", event);

        awaitUntilProcessed();

        Document updated = documentRepository.findById(document.getId()).orElseThrow();
        assertEquals(DocumentStatus.PROCESSED, updated.getStatus());
    }

    @Test
    void documentIngestionCompleted_missingDocument_doesNotSendDlq() throws Exception {
        BlockingQueue<ConsumerRecord<String, DocumentIngestionDlqMessage>> queue =
                createConsumer("document-ingestion-dlq-test", DocumentIngestionDlqMessage.class);

        DocumentIngestionCompletedEvent event = new DocumentIngestionCompletedEvent(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
        );

        kafkaTemplate.send("document-ingestion-completed-topic-test", event);

        DocumentIngestionDlqMessage dlqMessage = awaitMessage(queue, 3, TimeUnit.SECONDS);
        assertNull(dlqMessage);
    }

    @Test
    void workspaceMemberDataUpdate_updatesMembers() {
        Workspace workspace = testData.createWorkspace("Workspace", UUID.randomUUID(), "owner@example.com");
        WorkspaceMember member = testData.createWorkspaceMember(
                workspace.getId(),
                UUID.randomUUID(),
                "old@example.com",
                "Old Name",
                WorkspaceRole.VIEWER,
                WorkspaceMemberStatus.ACCEPTED
        );

        UpdateWorkspaceMemberDataEvent event = new UpdateWorkspaceMemberDataEvent(
                member.getUserId().toString(),
                "New",
                "Name",
                "new@example.com"
        );

        kafkaTemplate.send("update-workspace-member-data-topic-test", event);

        awaitUntilProcessed();

        WorkspaceMember updated = workspaceMemberRepository.findById(member.getId()).orElseThrow();
        assertEquals("new@example.com", updated.getEmail());
        assertEquals("New Name", updated.getUserName());
    }

    @Test
    void workspaceMemberDataUpdate_missingMember_doesNotSendDlq() throws Exception {
        BlockingQueue<ConsumerRecord<String, WorkspaceMemberDataUpdateDlqMessage>> queue =
                createConsumer("workspace-member-data-update-dlq-test", WorkspaceMemberDataUpdateDlqMessage.class);

        UpdateWorkspaceMemberDataEvent event = new UpdateWorkspaceMemberDataEvent(
                UUID.randomUUID().toString(),
                "New",
                "Name",
                "new@example.com"
        );

        kafkaTemplate.send("update-workspace-member-data-topic-test", event);

        WorkspaceMemberDataUpdateDlqMessage dlqMessage = awaitMessage(queue, 3, TimeUnit.SECONDS);
        assertNull(dlqMessage);
    }

    private void awaitUntilProcessed() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
