package com.baskaaleksander.nuvine.integration.controller;

import com.baskaaleksander.nuvine.application.dto.AdminUserListResponse;
import com.baskaaleksander.nuvine.application.dto.AdminUserResponse;
import com.baskaaleksander.nuvine.application.dto.PagedResponse;
import com.baskaaleksander.nuvine.domain.model.User;
import com.baskaaleksander.nuvine.integration.base.BaseControllerIntegrationTest;
import com.baskaaleksander.nuvine.integration.support.JwtTestUtils;
import com.baskaaleksander.nuvine.integration.support.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerIT extends BaseControllerIntegrationTest {

    @Autowired
    private TestDataBuilder testData;

    @BeforeEach
    void setUp() {
        testData.cleanUp();
    }

    @Test
    void getAllUsers_requiresAdminRole() {
        UUID userId = UUID.randomUUID();
        User user = testData.createUser(userId, "member@example.com");

        ResponseEntity<PagedResponse> unauthenticated = restTemplate.getForEntity(
                "/api/v1/auth/users",
                PagedResponse.class
        );
        assertEquals(401, unauthenticated.getStatusCode().value());

        JwtTestUtils jwtUtils = jwtTestUtils();
        String userJwt = jwtUtils.generateJwt(userId, user.getEmail(), List.of("ROLE_USER"));

        ResponseEntity<PagedResponse> forbidden = restTemplate.exchange(
                "/api/v1/auth/users",
                HttpMethod.GET,
                new HttpEntity<>(null, authHeaders(userJwt)),
                PagedResponse.class
        );
        assertEquals(403, forbidden.getStatusCode().value());
    }

    @Test
    void getAllUsers_returnsPagedResponse() {
        UUID userId = UUID.randomUUID();
        testData.createUser(userId, "adminlist@example.com");
        testData.createUser(UUID.randomUUID(), "adminlist2@example.com");

        JwtTestUtils jwtUtils = jwtTestUtils();
        String adminJwt = jwtUtils.generateJwt(userId, "adminlist@example.com", List.of("ROLE_ADMIN"));

        ResponseEntity<PagedResponse<AdminUserListResponse>> response = restTemplate.exchange(
                "/api/v1/auth/users",
                HttpMethod.GET,
                new HttpEntity<>(null, authHeaders(adminJwt)),
                new ParameterizedTypeReference<>() {}
        );

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().content().isEmpty());
    }

    @Test
    void getUserById_returnsUserAndRoles() {
        UUID userId = UUID.randomUUID();
        User user = testData.createUser(userId, "admin@example.com", "Admin", "User", true, true);

        wireMockStubs.stubRoleMappingGet(userId, List.of("ROLE_ADMIN", "ROLE_USER"));

        JwtTestUtils jwtUtils = jwtTestUtils();
        String adminJwt = jwtUtils.generateJwt(userId, user.getEmail(), List.of("ROLE_ADMIN"));

        ResponseEntity<AdminUserResponse> response = restTemplate.exchange(
                "/api/v1/auth/users/" + userId,
                HttpMethod.GET,
                new HttpEntity<>(null, authHeaders(adminJwt)),
                AdminUserResponse.class
        );

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(user.getEmail(), response.getBody().email());
        assertTrue(response.getBody().roles().contains("ROLE_ADMIN"));
    }

    private JwtTestUtils jwtTestUtils() {
        String issuer = "http://localhost:" + wireMockServer.port() + "/realms/nuvine";
        return new JwtTestUtils(issuer);
    }
}
