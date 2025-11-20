package com.baskaaleksander.nuvine.application.mapper;

import com.baskaaleksander.nuvine.application.dto.WorkspaceMemberResponse;
import com.baskaaleksander.nuvine.domain.model.WorkspaceMember;
import com.baskaaleksander.nuvine.domain.model.WorkspaceRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkspaceMemberMapperTest {

    private final WorkspaceMemberMapper mapper = new WorkspaceMemberMapper();

    @Test
    void toWorkspaceMemberResponse_mapsAllFields() {
        UUID id = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.now();

        WorkspaceMember member = WorkspaceMember.builder()
                .id(id)
                .workspaceId(workspaceId)
                .userId(userId)
                .role(WorkspaceRole.MODERATOR)
                .createdAt(createdAt)
                .build();

        WorkspaceMemberResponse response = mapper.toWorkspaceMemberResponse(member);

        assertEquals(id, response.id());
        assertEquals(workspaceId, response.workspaceId());
        assertEquals(userId, response.userId());
        assertEquals(WorkspaceRole.MODERATOR, response.role());
        assertEquals(createdAt, response.createdAt());
    }
}
