package com.baskaaleksander.nuvine.integration.controller;

import com.baskaaleksander.nuvine.application.dto.UserInternalResponse;
import com.baskaaleksander.nuvine.domain.model.User;
import com.baskaaleksander.nuvine.integration.base.BaseControllerIntegrationTest;
import com.baskaaleksander.nuvine.integration.support.JwtTestUtils;
import com.baskaaleksander.nuvine.integration.support.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InternalUserControllerIT extends BaseControllerIntegrationTest {

    @Autowired
    private TestDataBuilder testData;

    @BeforeEach
    void setUp() {
        testData.cleanUp();
    }

    @Test
    void getUser_requiresInternalRole() {
        UUID userId = UUID.randomUUID();
        User user = testData.createUser(userId, "internal@example.com");

        ResponseEntity<UserInternalResponse> unauthenticated = restTemplate.getForEntity(
                "/api/v1/internal/auth/users/" + userId,
                UserInternalResponse.class
        );
        assertEquals(401, unauthenticated.getStatusCode().value());

        JwtTestUtils jwtUtils = jwtTestUtils();
        String userJwt = jwtUtils.generateJwt(userId, user.getEmail(), List.of("ROLE_USER"));

        ResponseEntity<UserInternalResponse> forbidden = restTemplate.exchange(
                "/api/v1/internal/auth/users/" + userId,
                HttpMethod.GET,
                new HttpEntity<>(null, authHeaders(userJwt)),
                UserInternalResponse.class
        );
        assertEquals(403, forbidden.getStatusCode().value());

        String internalJwt = jwtUtils.generateJwt(userId, user.getEmail(), List.of("ROLE_INTERNAL_SERVICE"));

        ResponseEntity<UserInternalResponse> allowed = restTemplate.exchange(
                "/api/v1/internal/auth/users/" + userId,
                HttpMethod.GET,
                new HttpEntity<>(null, authHeaders(internalJwt)),
                UserInternalResponse.class
        );
        assertEquals(200, allowed.getStatusCode().value());
        assertNotNull(allowed.getBody());
        assertEquals(user.getEmail(), allowed.getBody().email());
    }

    @Test
    void getUserByEmail_requiresInternalRole() {
        UUID userId = UUID.randomUUID();
        User user = testData.createUser(userId, "internal-email@example.com");

        JwtTestUtils jwtUtils = jwtTestUtils();
        String internalJwt = jwtUtils.generateJwt(userId, user.getEmail(), List.of("ROLE_INTERNAL_SERVICE"));

        ResponseEntity<UserInternalResponse> response = restTemplate.exchange(
                "/api/v1/internal/auth/users/email?email=" + user.getEmail(),
                HttpMethod.GET,
                new HttpEntity<>(null, authHeaders(internalJwt)),
                UserInternalResponse.class
        );

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(user.getEmail(), response.getBody().email());
    }

    private JwtTestUtils jwtTestUtils() {
        String issuer = "http://localhost:" + wireMockServer.port() + "/realms/nuvine";
        return new JwtTestUtils(issuer);
    }
}
