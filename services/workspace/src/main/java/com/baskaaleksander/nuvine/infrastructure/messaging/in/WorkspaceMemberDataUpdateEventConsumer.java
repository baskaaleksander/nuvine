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
public class WorkspaceMemberDataUpdateEventConsumer {

    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceMemberDataUpdateDlqProducer dlqProducer;
    private final EntityCacheEvictionService entityCacheEvictionService;
    private final AccessCacheEvictionService accessCacheEvictionService;

    @Value("${topics.workspace-member-data-update-topic}")
    private String workspaceMemberDataUpdateTopic;

    @KafkaListener(topics = "${topics.workspace-member-data-update-topic}")
    @Transactional
    public void consumeWorkspaceMemberDataUpdateEvent(UpdateWorkspaceMemberDataEvent event) {
        log.info("WORKSPACE_MEMBER_DATA_UPDATE_EVENT received userId={}", event.userId());

        try {
            processEvent(event);
            log.info("WORKSPACE_MEMBER_DATA_UPDATE_EVENT processed userId={}", event.userId());
        } catch (Exception e) {
            log.error("WORKSPACE_MEMBER_DATA_UPDATE_EVENT failed userId={} error={}",
                    event.userId(), e.getMessage(), e);

            WorkspaceMemberDataUpdateDlqMessage dlqMessage =
                    WorkspaceMemberDataUpdateDlqMessage.createInitial(event, e, workspaceMemberDataUpdateTopic);
            dlqProducer.sendToDlq(dlqMessage);

            log.info("WORKSPACE_MEMBER_DATA_UPDATE_EVENT sent to DLQ userId={}", event.userId());
        }
    }

    private void processEvent(UpdateWorkspaceMemberDataEvent event) {
        UUID userId = UUID.fromString(event.userId());
        List<WorkspaceMember> members = workspaceMemberRepository.findAllByUserId(userId);

        if (members.isEmpty()) {
            log.info("WORKSPACE_MEMBER_DATA_UPDATE_EVENT no members found userId={}", event.userId());
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

        log.info("WORKSPACE_MEMBER_DATA_UPDATE_EVENT updated membersCount={} userId={}",
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
}
