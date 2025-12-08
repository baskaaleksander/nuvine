package com.baskaaleksander.nuvine.domain.security;

import com.baskaaleksander.nuvine.application.dto.WorkspaceMemberResponse;
import com.baskaaleksander.nuvine.domain.model.WorkspaceRole;
import com.baskaaleksander.nuvine.infrastructure.client.WorkspaceServiceUserClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("billingDataAccessEvaluator")
@RequiredArgsConstructor
@Slf4j
public class BillingDataAccessEvaluator {

    private final WorkspaceServiceUserClient workspaceServiceUserClient;

    public boolean canAccessBillingData(UUID workspaceId) {
        try {
            WorkspaceMemberResponse workspaceMemberResponse = workspaceServiceUserClient.getWorkspaceMember(workspaceId);
            log.debug("Workspace member response: {}", workspaceMemberResponse);
            if (workspaceMemberResponse.role() == WorkspaceRole.OWNER) {
                return true;
            }
        } catch (Exception e) {
            log.error("Failed to check workspace access for workspaceId={}: {}", workspaceId, e.getMessage(), e);
            return false;
        }
        return false;
    }
}
