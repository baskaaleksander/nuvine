package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.AdminUserListResponse;
import com.baskaaleksander.nuvine.application.dto.AdminUserResponse;
import com.baskaaleksander.nuvine.application.dto.PagedResponse;
import com.baskaaleksander.nuvine.application.dto.PaginationRequest;
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
                page.getNumber(),
                page.getTotalElements(),
                page.getSize(),
                page.getTotalPages(),
                page.isLast(),
                page.hasNext()
        );
    }

    public void validateUserExists(UUID id) {
        userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }
}
