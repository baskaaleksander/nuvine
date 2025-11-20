package com.baskaaleksander.nuvine.application.mapper;

import com.baskaaleksander.nuvine.application.dto.ProjectResponse;
import com.baskaaleksander.nuvine.domain.model.Project;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectMapperTest {

    private final ProjectMapper mapper = new ProjectMapper();

    @Test
    void toProjectResponse_mapsAllFields() {
        UUID id = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        Instant createdAt = Instant.now();

        Project project = Project.builder()
                .id(id)
                .name("Project")
                .description("Desc")
                .workspaceId(workspaceId)
                .createdAt(createdAt)
                .build();

        ProjectResponse response = mapper.toProjectResponse(project);

        assertEquals(id, response.id());
        assertEquals("Project", response.name());
        assertEquals("Desc", response.description());
        assertEquals(workspaceId, response.workspaceId());
        assertEquals(createdAt, response.createdAt());
    }
}
