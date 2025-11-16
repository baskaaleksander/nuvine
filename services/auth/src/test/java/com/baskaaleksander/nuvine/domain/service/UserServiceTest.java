package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.AdminUserListResponse;
import com.baskaaleksander.nuvine.application.dto.AdminUserResponse;
import com.baskaaleksander.nuvine.application.dto.PagedResponse;
import com.baskaaleksander.nuvine.application.dto.PaginationRequest;
import com.baskaaleksander.nuvine.application.mapping.AdminUserListMapper;
import com.baskaaleksander.nuvine.domain.exception.UserNotFoundException;
import com.baskaaleksander.nuvine.domain.model.User;
import com.baskaaleksander.nuvine.infrastructure.config.KeycloakClientProvider;
import com.baskaaleksander.nuvine.infrastructure.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private KeycloakClientProvider keycloakClientProvider;

    @Mock
    private AdminUserListMapper adminUserMapper;

    @InjectMocks
    private UserService service;

    private String userId;
    private UUID userUUID;
    private User userDb;

    private RoleRepresentation baseRole;
    private RoleRepresentation adminRole;
    private RoleRepresentation ignoredRole;

    private PaginationRequest request;
    private Pageable pageable;
    private User user1;
    private User user2;
    private AdminUserListResponse adminUserListResponse1;
    private AdminUserListResponse adminUserListResponse2;

    @BeforeEach
    void setUp() {
        userUUID = UUID.randomUUID();
        userId = userUUID.toString();

        userDb = User.builder()
                .id(userUUID)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .emailVerified(true)
                .onboardingCompleted(false)
                .build();

        baseRole = role("ROLE_USER");
        adminRole = role("ROLE_ADMIN");
        ignoredRole = role("IGNORED");

        ReflectionTestUtils.setField(service, "realm", "test-realm");

        request = new PaginationRequest();
        request.setPage(1);
        request.setSize(10);
        request.setSortField("id");
        request.setDirection(Sort.Direction.DESC);

        pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getDirection(),
                request.getSortField()
        );

        user1 = User.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .emailVerified(true)
                .onboardingCompleted(true)
                .build();

        user2 = User.builder()
                .id(UUID.randomUUID())
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@example.com")
                .emailVerified(false)
                .onboardingCompleted(false)
                .build();

        adminUserListResponse1 = new AdminUserListResponse(
                user1.getId(),
                user1.getFirstName(),
                user1.getLastName(),
                user1.getEmail(),
                user1.isEmailVerified(),
                user1.isOnboardingCompleted()
        );

        adminUserListResponse2 = new AdminUserListResponse(
                user2.getId(),
                user2.getFirstName(),
                user2.getLastName(),
                user2.getEmail(),
                user2.isEmailVerified(),
                user2.isOnboardingCompleted()
        );
    }

    private void mockKeycloakRoles(List<RoleRepresentation> roles) {
        Keycloak keycloak = mock(Keycloak.class);
        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        UserResource keycloakUser = mock(UserResource.class);
        RoleMappingResource roleMappingResource = mock(RoleMappingResource.class);
        MappingsRepresentation mappings = mock(MappingsRepresentation.class);

        when(keycloakClientProvider.getInstance()).thenReturn(keycloak);
        when(keycloak.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(userId)).thenReturn(keycloakUser);
        when(keycloakUser.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.getAll()).thenReturn(mappings);
        when(mappings.getRealmMappings()).thenReturn(roles);
    }

    @Test
    void shouldReturnAdminUserResponseWhenUserExists() {
        when(userRepository.findById(userUUID))
                .thenReturn(Optional.of(userDb));
        mockKeycloakRoles(List.of(adminRole, baseRole, ignoredRole));

        AdminUserResponse response = service.getUserById(userId);

        assertEquals(userDb.getId(), response.id());
        assertEquals("John", response.firstName());
        assertEquals("Doe", response.lastName());
        assertEquals("john@example.com", response.email());
        assertTrue(response.emailVerified());
        assertFalse(response.onboardingCompleted());
        assertEquals(List.of("ROLE_ADMIN", "ROLE_USER"), response.roles());
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenUserDoesNotExist() {
        when(userRepository.findById(userUUID))
                .thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.getUserById(userId));
    }

    @Test
    void shouldHandleEmptyRolesList() {
        when(userRepository.findById(userUUID))
                .thenReturn(Optional.of(userDb));
        mockKeycloakRoles(List.of());

        AdminUserResponse response = service.getUserById(userId);

        assertTrue(response.roles().isEmpty());
    }

    @Test
    void shouldFilterOnlyRolesStartingWithROLE() {
        when(userRepository.findById(userUUID))
                .thenReturn(Optional.of(userDb));
        mockKeycloakRoles(List.of(
                adminRole,
                role("NotARole"),
                baseRole
        ));

        AdminUserResponse response = service.getUserById(userId);

        assertEquals(List.of("ROLE_ADMIN", "ROLE_USER"), response.roles());
    }

    @Test
    void shouldReturnPagedUsersSuccessfully() {
        Pageable anyPageable = PageRequest.of(
                1,
                request.getSize(),
                request.getDirection(),
                request.getSortField()
        );

        Page<User> page = new PageImpl<>(
                List.of(user1, user2),
                anyPageable,
                2
        );

        when(userRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(adminUserMapper.toAdminUserListResponse(user1)).thenReturn(adminUserListResponse1);
        when(adminUserMapper.toAdminUserListResponse(user2)).thenReturn(adminUserListResponse2);

        PagedResponse<AdminUserListResponse> response = service.getAllUsers(request);

        assertEquals(2, response.content().size());
        assertEquals(2, response.totalPages());
        assertEquals(2, response.totalElements());
        assertEquals(10, response.size());
        assertEquals(1, response.page());
        assertTrue(response.last());
        assertFalse(response.next());
    }

    @Test
    void shouldReturnEmptyPageWhenNoUsers() {
        Page<User> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(userRepository.findAll(pageable)).thenReturn(emptyPage);

        PagedResponse<AdminUserListResponse> response = service.getAllUsers(request);

        assertTrue(response.content().isEmpty());
        assertEquals(0, response.totalElements());
        assertEquals(0, response.totalPages());
        assertEquals(10, response.size());
        assertEquals(1, response.page());
        assertTrue(response.last());
        assertFalse(response.next());
    }

    @Test
    void shouldCorrectlyMapUserEntitiesToAdminUserListResponse() {
        Page<User> page = new PageImpl<>(List.of(user1), pageable, 1);

        when(userRepository.findAll(pageable)).thenReturn(page);
        when(adminUserMapper.toAdminUserListResponse(user1)).thenReturn(adminUserListResponse1);

        PagedResponse<AdminUserListResponse> response = service.getAllUsers(request);

        AdminUserListResponse u = response.content().iterator().next();

        assertEquals(user1.getId(), u.id());
        assertEquals(user1.getFirstName(), u.firstName());
        assertEquals(user1.getLastName(), u.lastName());
        assertEquals(user1.getEmail(), u.email());
        assertEquals(user1.isEmailVerified(), u.emailVerified());
        assertEquals(user1.isOnboardingCompleted(), u.onboardingCompleted());
    }

    @Test
    void shouldPassCorrectPageableToRepository() {
        Page<User> page = new PageImpl<>(List.of(), pageable, 0);

        when(userRepository.findAll(pageable)).thenReturn(page);

        service.getAllUsers(request);

        verify(userRepository).findAll(pageable);
    }

    @Test
    void shouldHandleDifferentPageSizesAndNumbers() {
        PaginationRequest custom = new PaginationRequest();
        custom.setPage(3);
        custom.setSize(5);
        custom.setSortField("email");
        custom.setDirection(Sort.Direction.ASC);

        Pageable customPageable = PageRequest.of(
                custom.getPage(),
                custom.getSize(),
                custom.getDirection(),
                custom.getSortField()
        );

        Page<User> page = new PageImpl<>(List.of(user1), customPageable, 25);

        when(userRepository.findAll(customPageable)).thenReturn(page);
        when(adminUserMapper.toAdminUserListResponse(user1)).thenReturn(adminUserListResponse1);

        PagedResponse<AdminUserListResponse> response = service.getAllUsers(custom);

        assertEquals(25, response.totalElements());
        assertEquals(5, response.size());
        assertEquals(3, response.page());
    }

    private RoleRepresentation role(String name) {
        RoleRepresentation rep = new RoleRepresentation();
        rep.setName(name);
        return rep;
    }
}