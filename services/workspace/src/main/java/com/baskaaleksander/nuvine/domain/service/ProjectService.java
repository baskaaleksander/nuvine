package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.CreateProjectRequest;
import com.baskaaleksander.nuvine.domain.exception.ProjectAlreadyExistsException;
import com.baskaaleksander.nuvine.domain.model.Project;
import com.baskaaleksander.nuvine.infrastructure.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;

    public void createProject(UUID workspaceId, CreateProjectRequest request) {
        log.info("CREATE_PROJECT START workspaceId={}", workspaceId);
        if (projectRepository.existsByNameAndWorkspaceId(request.name(), workspaceId)) {
            log.info("CREATE_PROJECT FAILED reason=project_already_exists workspaceId={}", workspaceId);
            throw new ProjectAlreadyExistsException("Project already exists");
        }

        Project project;

        if (request.description() != null && !request.description().isEmpty()) {
            project = Project.builder()
                    .name(request.name())
                    .description(request.description())
                    .workspaceId(workspaceId)
                    .build();
        } else {
            project = Project.builder()
                    .name(request.name())
                    .workspaceId(workspaceId)
                    .build();
        }

        projectRepository.save(project);
        log.info("CREATE_PROJECT END workspaceId={}", workspaceId);
    }
}
