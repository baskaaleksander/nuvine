package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.*;
import com.baskaaleksander.nuvine.application.mapper.ProjectMapper;
import com.baskaaleksander.nuvine.application.pagination.PaginationUtil;
import com.baskaaleksander.nuvine.domain.exception.ProjectAlreadyExistsException;
import com.baskaaleksander.nuvine.domain.exception.ProjectNotFoundException;
import com.baskaaleksander.nuvine.domain.model.Project;
import com.baskaaleksander.nuvine.infrastructure.repository.DocumentRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private ProjectService projectService;

    private UUID workspaceId;
    private UUID projectId;
    private Project savedProject;
    private ProjectResponse projectResponse;
    private CreateProjectRequest createRequestWithDescription;
    private CreateProjectRequest createRequestWithoutDescription;

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();
        projectId = UUID.randomUUID();

        savedProject = Project.builder()
                .id(projectId)
                .name("Project Name")
                .description("Desc")
                .workspaceId(workspaceId)
                .deleted(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        projectResponse = new ProjectResponse(
                savedProject.getId(),
                savedProject.getName(),
                savedProject.getDescription(),
                savedProject.getWorkspaceId(),
                savedProject.getCreatedAt()
        );

        createRequestWithDescription = new CreateProjectRequest("Project Name", "Desc");
        createRequestWithoutDescription = new CreateProjectRequest("Project Name", null);
    }

    @Test
    void createProject_whenProjectExists_throwsProjectAlreadyExistsException() {
        when(projectRepository.existsByNameAndWorkspaceId(createRequestWithDescription.name(), workspaceId))
                .thenReturn(true);

        assertThrows(ProjectAlreadyExistsException.class,
                () -> projectService.createProject(workspaceId, createRequestWithDescription));

        verify(projectRepository).existsByNameAndWorkspaceId(createRequestWithDescription.name(), workspaceId);
        verify(projectRepository, never()).save(any(Project.class));
        verifyNoInteractions(projectMapper);
    }

    @Test
    void createProject_savesWithDescriptionAndMaps() {
        when(projectRepository.existsByNameAndWorkspaceId(createRequestWithDescription.name(), workspaceId))
                .thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenReturn(savedProject);
        when(projectMapper.toProjectResponse(savedProject)).thenReturn(projectResponse);

        ProjectResponse response = projectService.createProject(workspaceId, createRequestWithDescription);

        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        InOrder inOrder = inOrder(projectRepository, projectMapper);

        inOrder.verify(projectRepository).existsByNameAndWorkspaceId(createRequestWithDescription.name(), workspaceId);
        inOrder.verify(projectRepository).save(projectCaptor.capture());
        inOrder.verify(projectMapper).toProjectResponse(savedProject);

        Project persisted = projectCaptor.getValue();
        assertEquals(createRequestWithDescription.name(), persisted.getName());
        assertEquals(createRequestWithDescription.description(), persisted.getDescription());
        assertEquals(workspaceId, persisted.getWorkspaceId());
        assertEquals(projectResponse, response);
    }

    @Test
    void createProject_savesWithoutDescriptionWhenEmpty() {
        when(projectRepository.existsByNameAndWorkspaceId(createRequestWithoutDescription.name(), workspaceId))
                .thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenReturn(savedProject);
        when(projectMapper.toProjectResponse(savedProject)).thenReturn(projectResponse);

        ProjectResponse response = projectService.createProject(workspaceId, createRequestWithoutDescription);

        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(projectCaptor.capture());
        Project persisted = projectCaptor.getValue();

        assertEquals(createRequestWithoutDescription.name(), persisted.getName());
        assertEquals(workspaceId, persisted.getWorkspaceId());
        // description should be null when not provided
        assertNull(persisted.getDescription());
        assertEquals(projectResponse, response);
    }

    @Test
    void getProjects_returnsPagedResponseWithMappedProjects() {
        PaginationRequest request = new PaginationRequest(0, 5, "name", Sort.Direction.ASC);
        Pageable expectedPageable = PaginationUtil.getPageable(request);

        Page<Project> page = new PageImpl<>(List.of(savedProject), expectedPageable, 1);

        when(projectRepository.findAllByWorkspaceId(workspaceId, expectedPageable)).thenReturn(page);
        when(projectMapper.toProjectResponse(savedProject)).thenReturn(projectResponse);

        PagedResponse<ProjectResponse> response = projectService.getProjects(workspaceId, request);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(projectRepository).findAllByWorkspaceId(eq(workspaceId), pageableCaptor.capture());
        assertEquals(expectedPageable, pageableCaptor.getValue());

        assertEquals(1, response.content().size());
        assertEquals(projectResponse, response.content().iterator().next());
        assertEquals(page.getTotalPages(), response.totalPages());
        assertEquals(page.getTotalElements(), response.totalElements());
        assertEquals(page.getSize(), response.size());
        assertEquals(page.getNumber(), response.page());
        assertEquals(page.isLast(), response.last());
        assertEquals(page.hasNext(), response.next());
        verify(projectMapper).toProjectResponse(savedProject);
        verifyNoMoreInteractions(projectMapper);
    }

    @Test
    void getProjects_whenEmpty_returnsEmptyContent() {
        PaginationRequest request = new PaginationRequest(0, 5, "name", Sort.Direction.ASC);
        Pageable expectedPageable = PaginationUtil.getPageable(request);
        Page<Project> page = new PageImpl<>(List.of(), expectedPageable, 0);

        when(projectRepository.findAllByWorkspaceId(workspaceId, expectedPageable)).thenReturn(page);

        PagedResponse<ProjectResponse> response = projectService.getProjects(workspaceId, request);

        assertEquals(0, response.content().size());
        assertEquals(0, response.totalElements());
        assertEquals(0, response.totalPages());
        verifyNoInteractions(projectMapper);
    }
}
