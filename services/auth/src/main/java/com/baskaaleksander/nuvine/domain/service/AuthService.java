package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.*;
import com.baskaaleksander.nuvine.domain.exception.EmailExistsException;
import com.baskaaleksander.nuvine.domain.exception.UserNotFoundException;
import com.baskaaleksander.nuvine.domain.model.User;
import com.baskaaleksander.nuvine.infrastrucure.config.KeycloakClientProvider;
import com.baskaaleksander.nuvine.infrastrucure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final KeycloakClientProvider keycloakClientProvider;
    private final UserRepository repository;

    @Value("${keycloak.realm}")
    private String realm;

    private static String DEFAULT_ROLE = "USER";

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (repository.existsByEmail(request.email())) {
            throw new EmailExistsException("User with email " + request.email() + " already exists");
        }

        UserResponse userCreated = createUserInKeycloak(request);

        User user = User.builder()
                .id(userCreated.id())
                .email(request.email())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .onboardingCompleted(false)
                .emailVerified(false)
                .build();

        repository.save(user);

        return userCreated;
    }

    private UserResponse createUserInKeycloak(RegisterRequest request) {

        var userResource = keycloakClientProvider.getInstance()
                .realm(realm)
                .users();

        var existing = userResource.search(request.email(), true);

        if (!existing.isEmpty()) {
            throw new EmailExistsException("User with email " + request.email() + " already exists");
        }

        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.email());
        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEnabled(true);
        user.setEmailVerified(false);

        var response = userResource.create(user);

        if (response.getStatus() != 201) {
            throw new RuntimeException("Failed to create user in Keycloak: " + response.getStatusInfo().getReasonPhrase());
        }

        String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

        CredentialRepresentation passwordCred = new CredentialRepresentation();
        passwordCred.setTemporary(false);
        passwordCred.setType(CredentialRepresentation.PASSWORD);
        passwordCred.setValue(request.password());

        userResource.get(userId).resetPassword(passwordCred);

        var roles = assignRole(DEFAULT_ROLE, userId);

        response.close();

        return new UserResponse(
                UUID.fromString(userId),
                request.firstName(),
                request.lastName(),
                request.email(),
                roles
        );
    }

    private List<String> assignRole(String role, String userId) {
        var realmResource = keycloakClientProvider.getInstance().realm(realm);
        var userResource = realmResource.users();
        var rolesResource = realmResource.roles();

        RoleRepresentation userRole = rolesResource.get(role).toRepresentation();

        userResource.get(userId)
                .roles()
                .realmLevel()
                .add(List.of(userRole));

        return userResource.get(userId)
                .roles().
                getAll()
                .getRealmMappings()
                .stream()
                .map(RoleRepresentation::getName)
                .toList();
    }

    public KeycloakTokenResponse login(LoginRequest request) {
        return keycloakClientProvider.loginUser(request);
    }

    public KeycloakTokenResponse refreshToken(String refreshToken) {
        //todo refresh token rotation
        return keycloakClientProvider.refreshToken(refreshToken);
    }

    public MeResponse getMe(Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());

        User user = repository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        List<String> roles = List.of();
        if (realmAccess != null) {
            Object rolesObj = realmAccess.get("roles");
            if (rolesObj instanceof Collection<?> r) {
                roles = r.stream().map(Object::toString).filter(string -> string.startsWith("ROLE")).toList();
            }
        }

        return new MeResponse(
                userId,
                jwt.getClaimAsString("email"),
                user.getFirstName(),
                user.getLastName(),
                roles,
                jwt.getClaim("email_verified") != null
                ? jwt.getClaim("email_verified")
                        : user.isEmailVerified(),
                user.isOnboardingCompleted()
        );
    }
}
