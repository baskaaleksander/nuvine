package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.*;
import com.baskaaleksander.nuvine.application.mapper.ProjectMapper;
import com.baskaaleksander.nuvine.application.pagination.PaginationUtil;
import com.baskaaleksander.nuvine.domain.exception.ProjectAlreadyExistsException;
import com.baskaaleksander.nuvine.domain.exception.ProjectNotFoundException;
import com.baskaaleksander.nuvine.domain.model.Project;
import com.baskaaleksander.nuvine.infrastructure.repository.DocumentRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.ProjectRepository;
import org.mockito.InOrder;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private EntityCacheEvictionService entityCacheEvictionService;

    @InjectMocks
    private ProjectService projectService;

    private UUID workspaceId;
    private UUID projectId;
    private Project savedProject;
    private Project deletedProject;
    private ProjectResponse projectResponse;
    private ProjectDetailedResponse detailedResponse;
    private UpdateProjectRequest updateNameRequest;
    private UpdateProjectRequest updateDescriptionRequest;
    private UpdateProjectRequest updateBothRequest;
    private UpdateProjectRequest updateNoChangesRequest;
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

        deletedProject = Project.builder()
                .id(projectId)
                .name("Deleted Project")
                .description("Desc")
                .workspaceId(workspaceId)
                .deleted(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        detailedResponse = new ProjectDetailedResponse(
                savedProject.getId(),
                savedProject.getName(),
                savedProject.getDescription(),
                savedProject.getWorkspaceId(),
                5L,
                savedProject.getCreatedAt(),
                savedProject.getUpdatedAt(),
                savedProject.getVersion()
        );

        updateNameRequest = new UpdateProjectRequest("New Name", null);
        updateDescriptionRequest = new UpdateProjectRequest(null, "New Desc");
        updateBothRequest = new UpdateProjectRequest("New Name", "New Desc");
        updateNoChangesRequest = new UpdateProjectRequest("   ", savedProject.getDescription());

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

    @Test
    void getProjectById_whenNotFound_throwsProjectNotFoundException() {
        when(projectRepository.findById(projectId)).thenReturn(java.util.Optional.empty());

        assertThrows(ProjectNotFoundException.class, () -> projectService.getProjectById(projectId));

        verify(projectRepository).findById(projectId);
        verifyNoInteractions(documentRepository);
    }

    @Test
    void getProjectById_whenDeleted_throwsProjectNotFoundException() {
        when(projectRepository.findById(projectId)).thenReturn(java.util.Optional.of(deletedProject));

        assertThrows(ProjectNotFoundException.class, () -> projectService.getProjectById(projectId));

        verify(projectRepository).findById(projectId);
        verifyNoInteractions(documentRepository);
    }

    @Test
    void getProjectById_returnsDetailedResponseWithDocumentCount() {
        when(projectRepository.findById(projectId)).thenReturn(java.util.Optional.of(savedProject));
        when(documentRepository.getDocumentCountByProjectId(projectId)).thenReturn(5L);

        ProjectDetailedResponse response = projectService.getProjectById(projectId);

        assertEquals(detailedResponse, response);
        verify(projectRepository).findById(projectId);
        verify(documentRepository).getDocumentCountByProjectId(projectId);
    }

    @Test
    void updateProject_whenNotFound_throwsProjectNotFoundException() {
        when(projectRepository.findById(projectId)).thenReturn(java.util.Optional.empty());

        assertThrows(ProjectNotFoundException.class, () -> projectService.updateProject(projectId, updateNameRequest));

        verify(projectRepository).findById(projectId);
        verify(projectRepository, never()).save(any());
    }

    @Test
    void updateProject_whenDeleted_throwsProjectNotFoundException() {
        when(projectRepository.findById(projectId)).thenReturn(java.util.Optional.of(deletedProject));

        assertThrows(ProjectNotFoundException.class, () -> projectService.updateProject(projectId, updateNameRequest));

        verify(projectRepository).findById(projectId);
        verify(projectRepository, never()).save(any());
    }

    @Test
    void updateProject_updatesOnlyNameWhenChangedAndNotBlank() {
        when(projectRepository.findById(projectId)).thenReturn(java.util.Optional.of(savedProject));

        projectService.updateProject(projectId, updateNameRequest);

        InOrder inOrder = inOrder(projectRepository, entityCacheEvictionService);
        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        inOrder.verify(projectRepository).save(projectCaptor.capture());
        inOrder.verify(entityCacheEvictionService).evictProject(projectId);
        Project updated = projectCaptor.getValue();
        assertEquals(updateNameRequest.name(), updated.getName());
        assertEquals(savedProject.getDescription(), updated.getDescription());
    }

    @Test
    void updateProject_updatesOnlyDescriptionWhenChanged() {
        when(projectRepository.findById(projectId)).thenReturn(java.util.Optional.of(savedProject));

        projectService.updateProject(projectId, updateDescriptionRequest);

        InOrder inOrder = inOrder(projectRepository, entityCacheEvictionService);
        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        inOrder.verify(projectRepository).save(projectCaptor.capture());
        inOrder.verify(entityCacheEvictionService).evictProject(projectId);
        Project updated = projectCaptor.getValue();
        assertEquals(savedProject.getName(), updated.getName());
        assertEquals(updateDescriptionRequest.description(), updated.getDescription());
    }

    @Test
    void updateProject_updatesNameAndDescriptionWhenBothChanged() {
        when(projectRepository.findById(projectId)).thenReturn(java.util.Optional.of(savedProject));

        projectService.updateProject(projectId, updateBothRequest);

        InOrder inOrder = inOrder(projectRepository, entityCacheEvictionService);
        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        inOrder.verify(projectRepository).save(projectCaptor.capture());
        inOrder.verify(entityCacheEvictionService).evictProject(projectId);
        Project updated = projectCaptor.getValue();
        assertEquals(updateBothRequest.name(), updated.getName());
        assertEquals(updateBothRequest.description(), updated.getDescription());
    }

    @Test
    void updateProject_whenNoChanges_doesNotSave() {
        when(projectRepository.findById(projectId)).thenReturn(java.util.Optional.of(savedProject));

        projectService.updateProject(projectId, updateNoChangesRequest);

        verify(projectRepository, never()).save(any());
    }

    @Test
    void deleteProject_whenNotFound_throwsProjectNotFoundException() {
        when(projectRepository.findById(projectId)).thenReturn(java.util.Optional.empty());

        assertThrows(ProjectNotFoundException.class, () -> projectService.deleteProject(projectId));

        verify(projectRepository).findById(projectId);
        verify(projectRepository, never()).save(any());
    }

    @Test
    void deleteProject_whenAlreadyDeleted_throwsProjectNotFoundException() {
        when(projectRepository.findById(projectId)).thenReturn(java.util.Optional.of(deletedProject));

        assertThrows(ProjectNotFoundException.class, () -> projectService.deleteProject(projectId));

        verify(projectRepository).findById(projectId);
        verify(projectRepository, never()).save(any());
    }

    @Test
    void deleteProject_setsDeletedTrueAndSaves() {
        when(projectRepository.findById(projectId)).thenReturn(java.util.Optional.of(savedProject));

        projectService.deleteProject(projectId);

        InOrder inOrder = inOrder(projectRepository, entityCacheEvictionService);
        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        inOrder.verify(projectRepository).save(projectCaptor.capture());
        inOrder.verify(entityCacheEvictionService).evictProject(projectId);
        assertTrue(projectCaptor.getValue().isDeleted());
    }
}
