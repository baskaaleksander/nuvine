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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    private final DocumentRepository documentRepository;

    public ProjectResponse createProject(UUID workspaceId, CreateProjectRequest request) {
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

        log.info("CREATE_PROJECT END workspaceId={}", workspaceId);
        return projectMapper.toProjectResponse(projectRepository.save(project));
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
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getWorkspaceId(),
                documentCount,
                project.getCreatedAt(),
                project.getUpdatedAt(),
                project.getVersion()
        );
    }

    @Transactional
    public void updateProject(UUID projectId, UpdateProjectRequest request) {
        log.info("UPDATE_PROJECT START projectId={}", projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

        if (project.isDeleted()) {
            log.info("UPDATE_PROJECT FAILED reason=project_not_found projectId={}", projectId);
            throw new ProjectNotFoundException("Project not found");
        }

        boolean updated = false;
        boolean nameChanged = false;
        boolean descriptionChanged = false;

        if (request.name() != null) {
            String trimmedName = request.name().trim();

            if (!trimmedName.isEmpty() && !Objects.equals(project.getName(), trimmedName)) {
                project.setName(trimmedName);
                updated = true;
                nameChanged = true;
            }
        }

        if (request.description() != null
                && !Objects.equals(project.getDescription(), request.description())) {
            project.setDescription(request.description());
            updated = true;
            descriptionChanged = true;
        }

        if (updated) {
            projectRepository.save(project);

            log.info("UPDATE_PROJECT SUCCESS projectId={} nameChanged={} descriptionChanged={}",
                    projectId, nameChanged, descriptionChanged);
        } else {
            log.info("UPDATE_PROJECT NO_CHANGES projectId={}", projectId);
        }

        log.info("UPDATE_PROJECT END projectId={}", projectId);
    }
}
