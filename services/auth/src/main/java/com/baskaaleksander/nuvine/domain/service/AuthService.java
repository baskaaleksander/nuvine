package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.LoginRequest;
import com.baskaaleksander.nuvine.application.dto.TokenResponse;
import com.baskaaleksander.nuvine.application.dto.UserResponse;
import com.baskaaleksander.nuvine.application.dto.RegisterRequest;
import com.baskaaleksander.nuvine.domain.exception.EmailExistsException;
import com.baskaaleksander.nuvine.domain.model.User;
import com.baskaaleksander.nuvine.infrastrucure.config.KeycloakClientProvider;
import com.baskaaleksander.nuvine.infrastrucure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    public TokenResponse login(LoginRequest request) {
        return keycloakClientProvider.loginUser(request);
    }
}
