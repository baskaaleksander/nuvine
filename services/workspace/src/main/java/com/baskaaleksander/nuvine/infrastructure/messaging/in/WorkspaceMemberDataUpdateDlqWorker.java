package com.baskaaleksander.nuvine.infrastructure.messaging.in;

import com.baskaaleksander.nuvine.domain.model.WorkspaceMember;
import com.baskaaleksander.nuvine.domain.service.AccessCacheEvictionService;
import com.baskaaleksander.nuvine.domain.service.EntityCacheEvictionService;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.UpdateWorkspaceMemberDataEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.WorkspaceMemberDataUpdateDlqMessage;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.WorkspaceMemberDataUpdateDlqProducer;
import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkspaceMemberDataUpdateDlqWorker {

    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceMemberDataUpdateDlqProducer dlqProducer;
    private final EntityCacheEvictionService entityCacheEvictionService;
    private final AccessCacheEvictionService accessCacheEvictionService;

    @Value("${dlq.workspace-member-data-update.max-retry-attempts:10}")
    private int maxRetryAttempts;

    @KafkaListener(
            topics = "${topics.workspace-member-data-update-dlq-topic}",
            containerFactory = "workspaceMemberDataUpdateDlqKafkaListenerContainerFactory"
    )
    public void processDlqBatch(List<WorkspaceMemberDataUpdateDlqMessage> messages) {
        log.info("WORKSPACE_MEMBER_DATA_UPDATE_DLQ_WORKER BATCH RECEIVED size={}", messages.size());

        for (WorkspaceMemberDataUpdateDlqMessage dlqMessage : messages) {
            processMessage(dlqMessage);
        }

        log.info("WORKSPACE_MEMBER_DATA_UPDATE_DLQ_WORKER BATCH PROCESSED size={}", messages.size());
    }

    @Transactional
    protected void processMessage(WorkspaceMemberDataUpdateDlqMessage dlqMessage) {
        String userId = dlqMessage.originalEvent().userId();
        int attemptCount = dlqMessage.attemptCount();

        log.info("WORKSPACE_MEMBER_DATA_UPDATE_DLQ_WORKER PROCESSING userId={} attemptCount={}",
                userId, attemptCount);

        try {
            processEvent(dlqMessage.originalEvent());
            log.info("WORKSPACE_MEMBER_DATA_UPDATE_DLQ_WORKER SUCCESS userId={} attemptCount={}",
                    userId, attemptCount);
        } catch (Exception e) {
            log.error("WORKSPACE_MEMBER_DATA_UPDATE_DLQ_WORKER FAILED userId={} attemptCount={} error={}",
                    userId, attemptCount, e.getMessage(), e);

            handleRetryOrDeadLetter(dlqMessage, e);
        }
    }

    private void processEvent(UpdateWorkspaceMemberDataEvent event) {
        UUID userId = UUID.fromString(event.userId());
        List<WorkspaceMember> members = workspaceMemberRepository.findAllByUserId(userId);

        if (members.isEmpty()) {
            log.info("WORKSPACE_MEMBER_DATA_UPDATE_DLQ_WORKER no members found userId={}", event.userId());
            return;
        }

        String userName = buildUserName(event.firstName(), event.lastName());

        for (WorkspaceMember member : members) {
            boolean updated = false;

            if (event.email() != null) {
                member.setEmail(event.email());
                updated = true;
            }

            if (userName != null) {
                member.setUserName(userName);
                updated = true;
            }

            if (updated) {
                workspaceMemberRepository.save(member);
                entityCacheEvictionService.evictWorkspaceMember(member.getWorkspaceId(), userId);
                accessCacheEvictionService.evictAccessForUserInWorkspace(member.getWorkspaceId(), userId);
            }
        }

        log.info("WORKSPACE_MEMBER_DATA_UPDATE_DLQ_WORKER updated membersCount={} userId={}",
                members.size(), event.userId());
    }

    private String buildUserName(String firstName, String lastName) {
        if (firstName == null && lastName == null) {
            return null;
        }
        if (firstName == null) {
            return lastName;
        }
        if (lastName == null) {
            return firstName;
        }
        return firstName + " " + lastName;
    }

    private void handleRetryOrDeadLetter(WorkspaceMemberDataUpdateDlqMessage dlqMessage, Exception e) {
        String userId = dlqMessage.originalEvent().userId();
        WorkspaceMemberDataUpdateDlqMessage updatedMessage = dlqMessage.incrementAttempt(e);

        if (updatedMessage.attemptCount() >= maxRetryAttempts) {
            log.error("WORKSPACE_MEMBER_DATA_UPDATE_DLQ_WORKER MAX_RETRIES_EXCEEDED userId={} attemptCount={} firstFailedAt={} - moving to dead letter",
                    userId, updatedMessage.attemptCount(), dlqMessage.firstFailedAt());

            dlqProducer.sendToDeadLetter(updatedMessage);
        } else {
            log.warn("WORKSPACE_MEMBER_DATA_UPDATE_DLQ_WORKER RETRY_SCHEDULED userId={} attemptCount={} nextAttempt={}",
                    userId, updatedMessage.attemptCount(), updatedMessage.attemptCount() + 1);

            dlqProducer.sendToDlq(updatedMessage);
        }
    }
}
