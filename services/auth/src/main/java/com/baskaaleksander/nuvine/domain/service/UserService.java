package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.*;
import com.baskaaleksander.nuvine.application.mapping.AdminUserListMapper;
import com.baskaaleksander.nuvine.application.pagination.PaginationUtil;
import com.baskaaleksander.nuvine.domain.exception.UserNotFoundException;
import com.baskaaleksander.nuvine.domain.model.User;
import com.baskaaleksander.nuvine.infrastructure.config.KeycloakClientProvider;
import com.baskaaleksander.nuvine.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;


@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final KeycloakClientProvider keycloakClientProvider;
    private final AdminUserListMapper adminUserMapper;

    @Value("${keycloak.realm}")
    private String realm;


    @Cacheable(value = "users-admin", key = "#userId")
    public AdminUserResponse getUserById(String userId) {

        log.info("GET_USER_BY_ID START userId={}", userId);

        User userDb = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        var realmResource = keycloakClientProvider.getInstance().realm(realm);
        var userResource = realmResource.users();
        var user = userResource.get(userId);

        List<String> roles = user
                .roles()
                .getAll()
                .getRealmMappings()
                .stream()
                .map(RoleRepresentation::getName)
                .filter(string -> string.startsWith("ROLE"))
                .toList();

        log.info("GET_USER_BY_ID SUCCESS userId={}", userDb.getId());

        return new AdminUserResponse(
                userDb.getId(),
                userDb.getFirstName(),
                userDb.getLastName(),
                userDb.getEmail(),
                userDb.isEmailVerified(),
                userDb.isOnboardingCompleted(),
                roles
        );
    }

    public PagedResponse<AdminUserListResponse> getAllUsers(PaginationRequest request) {
        log.info("GET_ALL_USERS START");
        Pageable pageable = PaginationUtil.getPageable(request);
        Page<User> page = userRepository.findAll(pageable);

        List<AdminUserListResponse> content = page.getContent().stream()
                .map(adminUserMapper::toAdminUserListResponse)
                .toList();

        log.info("GET_ALL_USERS SUCCESS");

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

    @Cacheable(value = "users-internal", key = "#id")
    public UserInternalResponse checkInternalUser(UUID id) {
        log.info("GET_USER_INTERNAL START userId={}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.info("GET_USER_INTERNAL FAILED reason=user_not_found userId={}", id);
                    return new UserNotFoundException("User not found");
                });

        log.info("GET_USER_INTERNAL END userId={}", id);

        return new UserInternalResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail()
        );
    }

    public UserInternalResponse checkInternalUserByEmail(String email) {
        log.info("GET_USER_INTERNAL_BY_EMAIL START");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.info("GET_USER_INTERNAL_BY_EMAIL FAILED reason=user_not_found");
                    return new UserNotFoundException("User not found");
                });

        log.info("GET_USER_INTERNAL_BY_EMAIL userId={}", user.getId());

        return new UserInternalResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail()
        );
    }
}
