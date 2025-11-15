package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.KeycloakTokenResponse;
import com.baskaaleksander.nuvine.application.dto.LoginRequest;
import com.baskaaleksander.nuvine.application.dto.MeResponse;
import com.baskaaleksander.nuvine.application.dto.RegisterRequest;
import com.baskaaleksander.nuvine.application.dto.UpdateMeRequest;
import com.baskaaleksander.nuvine.application.dto.UserResponse;
import com.baskaaleksander.nuvine.domain.exception.EmailExistsException;
import com.baskaaleksander.nuvine.domain.exception.InvalidTokenException;
import com.baskaaleksander.nuvine.domain.exception.TokenNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.UserNotFoundException;
import com.baskaaleksander.nuvine.domain.model.RefreshToken;
import com.baskaaleksander.nuvine.domain.model.User;
import com.baskaaleksander.nuvine.infrastructure.config.KeycloakClientProvider;
import com.baskaaleksander.nuvine.infrastructure.messaging.UserRegisteredEventProducer;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.UserRegisteredEvent;
import com.baskaaleksander.nuvine.infrastructure.repository.RefreshTokenRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private KeycloakClientProvider keycloakClientProvider;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private UserRegisteredEventProducer userRegisteredEventProducer;
    @Mock
    private EmailVerificationTokenGenerationService tokenGenerationService;
    @Mock
    private Keycloak keycloak;
    @Mock
    private RealmResource realmResource;
    @Mock
    private UsersResource usersResource;
    @Mock
    private UserResource userResource;
    @Mock
    private RolesResource rolesResource;
    @Mock
    private RoleResource roleResource;
    @Mock
    private RoleMappingResource roleMappingResource;
    @Mock
    private RoleScopeResource roleScopeResource;
    @Mock
    private MappingsRepresentation mappingsRepresentation;
    @Mock
    private RoleRepresentation roleRepresentation;
    @Mock
    private Response keycloakResponse;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User userEntity;
    private static final String REALM = "test-realm";
    private UserResponse keycloakUserResponse;
    private KeycloakTokenResponse loginResponse;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("John", "Doe", "john@example.com", "Secret123!");
        userEntity = User.builder()
                .id(UUID.randomUUID())
                .email(registerRequest.email())
                .firstName(registerRequest.firstName())
                .lastName(registerRequest.lastName())
                .onboardingCompleted(false)
                .emailVerified(false)
                .build();
        loginRequest = new LoginRequest(registerRequest.email(), registerRequest.password());
        loginResponse = new KeycloakTokenResponse("access", "refresh-token", 3600L, "Bearer", "openid");
        keycloakUserResponse = new UserResponse(userEntity.getId(),
                registerRequest.firstName(),
                registerRequest.lastName(),
                registerRequest.email(),
                List.of("ROLE_USER"));
        ReflectionTestUtils.setField(authService, "realm", REALM);
    }

    @Test
    void registerThrowsWhenEmailAlreadyExistsInDatabase() {
        when(userRepository.existsByEmail(registerRequest.email())).thenReturn(true);

        assertThrows(EmailExistsException.class, () -> authService.register(registerRequest));

        verifyNoInteractions(keycloakClientProvider);
    }

    @Test
    void registerCreatesUserSavesEntityAndSendsEvent() {
        when(userRepository.existsByEmail(registerRequest.email())).thenReturn(false);
        mockKeycloakCreateUserFlow();
        when(userRepository.save(any(User.class))).thenReturn(userEntity);
        when(tokenGenerationService.createToken(userEntity)).thenReturn(
                com.baskaaleksander.nuvine.domain.model.EmailVerificationToken.builder()
                        .token("token")
                        .user(userEntity)
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .build()
        );

        UserResponse response = authService.register(registerRequest);

        assertEquals(userEntity.getId(), response.id());
        assertEquals(registerRequest.email(), response.email());

        verify(userRepository).save(any(User.class));
        verify(tokenGenerationService).createToken(userEntity);
        ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(userRegisteredEventProducer).sendUserRegisteredEvent(eventCaptor.capture());
        UserRegisteredEvent event = eventCaptor.getValue();
        assertEquals(registerRequest.email(), event.email());
        assertEquals(userEntity.getId().toString(), event.userId());
    }

    @Test
    void loginThrowsWhenUserNotFoundLocally() {
        when(keycloakClientProvider.loginUser(loginRequest)).thenReturn(loginResponse);
        when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> authService.login(loginRequest));

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void loginPersistsRefreshTokenForUser() {
        when(keycloakClientProvider.loginUser(loginRequest)).thenReturn(loginResponse);
        when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(userEntity));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.login(loginRequest);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertEquals(loginResponse.refreshToken(), saved.getToken());
        assertEquals(userEntity, saved.getUser());
        assertEquals(false, saved.getRevoked());
    }

    @Test
    void refreshTokenThrowsWhenTokenNotFound() {
        when(refreshTokenRepository.findByToken("token")).thenReturn(Optional.empty());

        assertThrows(TokenNotFoundException.class, () -> authService.refreshToken("token"));
    }

    @Test
    void refreshTokenThrowsWhenTokenRevokedOrExpired() {
        RefreshToken stored = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token("token")
                .revoked(true)
                .expiresAt(Instant.now().plusSeconds(3600))
                .user(userEntity)
                .build();
        when(refreshTokenRepository.findByToken("token")).thenReturn(Optional.of(stored));

        assertThrows(InvalidTokenException.class, () -> authService.refreshToken("token"));

        stored.setRevoked(false);
        stored.setExpiresAt(Instant.now().minusSeconds(10));
        when(refreshTokenRepository.findByToken("token")).thenReturn(Optional.of(stored));

        assertThrows(InvalidTokenException.class, () -> authService.refreshToken("token"));
    }

    @Test
    void refreshTokenCreatesNewTokenWhenValid() {
        RefreshToken stored = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token("old-token")
                .revoked(false)
                .expiresAt(Instant.now().plusSeconds(3600))
                .user(userEntity)
                .build();
        KeycloakTokenResponse refreshed = new KeycloakTokenResponse("access2", "new-refresh", 3600L, "Bearer", "openid");
        when(refreshTokenRepository.findByToken("old-token")).thenReturn(Optional.of(stored));
        when(keycloakClientProvider.refreshToken("old-token")).thenReturn(refreshed);
        when(userRepository.findByEmail(userEntity.getEmail())).thenReturn(Optional.of(userEntity));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.refreshToken("old-token");

        verify(refreshTokenRepository).revokeToken("old-token");
        verify(refreshTokenRepository).updateUsedAt(any(Instant.class), any(UUID.class));
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertEquals(refreshed.refreshToken(), captor.getValue().getToken());
    }

    @Test
    void logoutAllDoesNothingWhenTokenMissing() {
        when(refreshTokenRepository.findByToken("token")).thenReturn(Optional.empty());

        authService.logoutAll("token");

        verify(refreshTokenRepository, never()).revokeAllTokensByEmail(anyString());
    }

    @Test
    void logoutAllRevokesTokensForUserEmail() {
        RefreshToken stored = RefreshToken.builder()
                .token("token")
                .user(userEntity)
                .build();
        when(refreshTokenRepository.findByToken("token")).thenReturn(Optional.of(stored));

        authService.logoutAll("token");

        verify(refreshTokenRepository).revokeAllTokensByEmail(userEntity.getEmail());
    }

    @Test
    void logoutRevokesTokenAndSwallowsErrors() {
        authService.logout("token");
        verify(refreshTokenRepository).revokeToken("token");

        doThrow(new RuntimeException("error")).when(refreshTokenRepository).revokeToken("token");
        assertDoesNotThrow(() -> authService.logout("token"));
    }

    @Test
    void getMeThrowsWhenUserMissing() {
        Jwt jwt = buildJwt(userEntity.getId(), userEntity.getEmail(), Map.of());
        when(userRepository.findById(userEntity.getId())).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> authService.getMe(jwt));
    }

    @Test
    void getMeReturnsUserDetailsFromClaimsAndDatabase() {
        userEntity.setFirstName("Jane");
        userEntity.setLastName("Smith");
        userEntity.setOnboardingCompleted(true);
        when(userRepository.findById(userEntity.getId())).thenReturn(Optional.of(userEntity));
        Jwt jwt = buildJwt(
                userEntity.getId(),
                userEntity.getEmail(),
                Map.of(
                        "realm_access", Map.of("roles", List.of("ROLE_USER", "OTHER")),
                        "email_verified", true
                )
        );

        MeResponse me = authService.getMe(jwt);

        assertEquals(userEntity.getFirstName(), me.firstName());
        assertEquals(List.of("ROLE_USER"), me.roles());
        assertTrue(me.emailVerified());
        assertEquals(userEntity.isOnboardingCompleted(), me.onboardingCompleted());
    }

    @Test
    void updateMeThrowsWhenUserNotFound() {
        Jwt jwt = buildJwt(userEntity.getId(), userEntity.getEmail(), Map.of());
        when(userRepository.findById(userEntity.getId())).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> authService.updateMe(jwt, new UpdateMeRequest("New", "Name")));
    }

    @Test
    void updateMeUpdatesNamesAndReturnsMe() {
        Jwt jwt = buildJwt(userEntity.getId(), userEntity.getEmail(), Map.of());
        when(userRepository.findById(userEntity.getId()))
                .thenReturn(Optional.of(userEntity), Optional.of(userEntity));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MeResponse me = authService.updateMe(jwt, new UpdateMeRequest("NewName", "NewLast"));

        verify(userRepository).save(userEntity);
        assertEquals("NewName", userEntity.getFirstName());
        assertEquals("NewLast", userEntity.getLastName());
        assertEquals("NewName", me.firstName());
        assertEquals("NewLast", me.lastName());
    }

    private Jwt buildJwt(UUID userId, String email, Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .subject(userId.toString())
                .header("alg", "none")
                .claim("email", email);
        claims.forEach(builder::claim);
        return builder.build();
    }

    private void mockKeycloakCreateUserFlow() {
        when(keycloakClientProvider.getInstance()).thenReturn(keycloak);
        when(keycloak.realm(REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.search(registerRequest.email(), true)).thenReturn(List.of());
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(keycloakResponse);
        when(keycloakResponse.getStatus()).thenReturn(201);
        when(keycloakResponse.getLocation()).thenReturn(URI.create("http://keycloak/admin/realms/test/users/" + userEntity.getId()));
        doNothing().when(keycloakResponse).close();
        when(usersResource.get(userEntity.getId().toString())).thenReturn(userResource);
        doNothing().when(userResource).resetPassword(any(CredentialRepresentation.class));
        when(realmResource.roles()).thenReturn(rolesResource);
        when(rolesResource.get("ROLE_USER")).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(roleRepresentation);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
        doNothing().when(roleScopeResource).add(any());
        when(roleMappingResource.getAll()).thenReturn(mappingsRepresentation);
        when(mappingsRepresentation.getRealmMappings()).thenReturn(List.of(roleRepresentation));
        when(roleRepresentation.getName()).thenReturn("ROLE_USER");
    }
}
