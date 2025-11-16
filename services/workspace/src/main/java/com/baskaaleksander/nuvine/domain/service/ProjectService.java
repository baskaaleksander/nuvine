package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.*;
import com.baskaaleksander.nuvine.application.mapper.ProjectMapper;
import com.baskaaleksander.nuvine.application.pagination.PaginationUtil;
import com.baskaaleksander.nuvine.domain.exception.ProjectAlreadyExistsException;
import com.baskaaleksander.nuvine.domain.exception.ProjectNotFoundException;
import com.baskaaleksander.nuvine.domain.model.Project;
import com.baskaaleksander.nuvine.infrastructure.repository.DocumentRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    private final DocumentRepository documentRepository;

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

    public PagedResponse<ProjectResponse> getProjects(UUID workspaceId, PaginationRequest request) {
        Pageable pageable = PaginationUtil.getPageable(request);
        Page<Project> page = projectRepository.findAllByWorkspaceId(workspaceId, pageable);

        List<ProjectResponse> content = page.getContent().stream()
                .map(projectMapper::toProjectResponse)
                .toList();

        return new PagedResponse<>(
                content,
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize(),
                page.getNumber(),
                page.isLast(),
                page.hasNext()
        );
    }

    public ProjectDetailedResponse getProjectById(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

        if (project.isDeleted()) {
            throw new ProjectNotFoundException("Project not found");
        }

        Long documentCount = documentRepository.getDocumentCountByProjectId(projectId);

        return new ProjectDetailedResponse(
                project.getName(),
                project.getDescription(),
                project.getWorkspaceId(),
                documentCount,
                project.getCreatedAt(),
                project.getUpdatedAt(),
                project.getVersion()
        );
    }
}
