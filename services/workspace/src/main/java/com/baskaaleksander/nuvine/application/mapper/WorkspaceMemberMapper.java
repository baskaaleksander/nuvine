package com.baskaaleksander.nuvine.application.mapper;

import com.baskaaleksander.nuvine.application.dto.WorkspaceMemberResponse;
import com.baskaaleksander.nuvine.domain.model.WorkspaceMember;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceMemberMapper {

    public WorkspaceMemberResponse toWorkspaceMemberResponse(WorkspaceMember workspaceMember) {
        return new WorkspaceMemberResponse(
                workspaceMember.getId() != null ? workspaceMember.getId() : null,
                workspaceMember.getWorkspaceId(),
                workspaceMember.getUserId() != null ? workspaceMember.getUserId() : null,
                workspaceMember.getEmail(),
                workspaceMember.getUserName() != null ? workspaceMember.getUserName() : null,
                workspaceMember.getRole(),
                workspaceMember.getStatus(),
                workspaceMember.getCreatedAt()
        );
    }
}
