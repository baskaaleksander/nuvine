package com.baskaaleksander.nuvine.application.mapper;

import com.baskaaleksander.nuvine.application.dto.WorkspaceMemberResponse;
import com.baskaaleksander.nuvine.domain.model.WorkspaceMember;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceMemberMapper {

    public WorkspaceMemberResponse toWorkspaceMemberResponse(WorkspaceMember workspaceMember) {
        return new WorkspaceMemberResponse(
                workspaceMember.getId(),
                workspaceMember.getWorkspaceId(),
                workspaceMember.getUserId(),
                workspaceMember.getRole(),
                workspaceMember.getCreatedAt()
        );
    }
}
