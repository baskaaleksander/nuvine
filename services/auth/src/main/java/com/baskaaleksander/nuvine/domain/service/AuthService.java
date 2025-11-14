package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.*;
import com.baskaaleksander.nuvine.application.util.MaskingUtil;
import com.baskaaleksander.nuvine.domain.exception.EmailExistsException;
import com.baskaaleksander.nuvine.domain.exception.InvalidTokenException;
import com.baskaaleksander.nuvine.domain.exception.TokenNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.UserNotFoundException;
import com.baskaaleksander.nuvine.domain.model.RefreshToken;
import com.baskaaleksander.nuvine.domain.model.User;
import com.baskaaleksander.nuvine.infrastrucure.config.KeycloakClientProvider;
import com.baskaaleksander.nuvine.infrastrucure.repository.RefreshTokenRepository;
import com.baskaaleksander.nuvine.infrastrucure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final KeycloakClientProvider keycloakClientProvider;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${keycloak.realm}")
    private String realm;

    private static String DEFAULT_ROLE = "ROLE_USER";

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            log.info("User registration failed: email taken email={}", MaskingUtil.maskEmail(request.email()));
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

        userRepository.save(user);

        log.info("User registered id={} email={}", userCreated.id(), MaskingUtil.maskEmail(userCreated.email()));

        return userCreated;
    }

    private UserResponse createUserInKeycloak(RegisterRequest request) {

        var userResource = keycloakClientProvider.getInstance()
                .realm(realm)
                .users();

        var existing = userResource.search(request.email(), true);

        if (!existing.isEmpty()) {
            log.info("User registration failed: email taken email={}", MaskingUtil.maskEmail(request.email()));
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
            log.error("Failed to create user in Keycloak: " + response.getStatusInfo().getReasonPhrase());
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

        log.info("User created in Keycloak id={} email={}", userId, MaskingUtil.maskEmail(request.email()));

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

    @Transactional
    public KeycloakTokenResponse login(LoginRequest request) {

        log.info("User login attempt email={}", MaskingUtil.maskEmail(request.email()));
        var response = keycloakClientProvider.loginUser(request);

        String refreshToken = response.getRefreshToken();

        RefreshToken token = RefreshToken.builder()
                .token(refreshToken)
                .expiresAt(Instant.now().plusSeconds(24 * 60 * 60))
                .user(
                        userRepository.findByEmail(request.email())
                                .orElseThrow(() -> new UserNotFoundException("User not found"))
                )
                .revoked(false)
                .build();

        refreshTokenRepository.save(token);

        log.info("User logged in email={}", request.email());

        return response;
    }

    @Transactional
    public KeycloakTokenResponse refreshToken(String refreshToken) {

        RefreshToken dbToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new TokenNotFoundException("Refresh token not found"));

        if (dbToken == null || dbToken.getRevoked() || dbToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token not found");
        }

        var response = keycloakClientProvider.refreshToken(refreshToken);

        String email = dbToken.getUser().getEmail();

        String newRefreshToken = response.getRefreshToken();


        refreshTokenRepository.revokeToken(refreshToken);
        refreshTokenRepository.updateUsedAt(Instant.now(), dbToken.getId());

        RefreshToken token = RefreshToken.builder()
                .token(newRefreshToken)
                .expiresAt(Instant.now().plusSeconds(24 * 60 * 60))
                .user(
                        userRepository.findByEmail(email)
                                .orElseThrow(() -> new UserNotFoundException("User not found"))
                )
                .revoked(false)
                .build();

        refreshTokenRepository.save(token);

        log.info("User refreshed token email={}", MaskingUtil.maskEmail(email));


        return response;
    }

    public void logoutAll(String refreshToken) {
        var dbToken = refreshTokenRepository.findByToken(refreshToken);

        dbToken.ifPresent(token -> {
            log.info("User logged out email={}", MaskingUtil.maskEmail(token.getUser().getEmail()));
            refreshTokenRepository.revokeAllTokensByEmail(token.getUser().getEmail());
        });
    }

    public void logout(String refreshToken) {
        try {
            refreshTokenRepository.revokeToken(refreshToken);
        } catch (Exception ignored) {
        }
    }

    public MeResponse getMe(Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());

        log.info("User getMe email={}", MaskingUtil.maskEmail(jwt.getClaimAsString("email")));

        User user = userRepository.findById(userId)
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
