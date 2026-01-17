package com.baskaaleksander.nuvine.integration.controller;

import com.baskaaleksander.nuvine.application.dto.LoginRequest;
import com.baskaaleksander.nuvine.application.dto.MeResponse;
import com.baskaaleksander.nuvine.application.dto.RegisterRequest;
import com.baskaaleksander.nuvine.application.dto.TokenResponse;
import com.baskaaleksander.nuvine.application.dto.UpdateMeRequest;
import com.baskaaleksander.nuvine.application.dto.UserResponse;
import com.baskaaleksander.nuvine.domain.model.User;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.UserRegisteredEvent;
import com.baskaaleksander.nuvine.infrastructure.repository.EmailVerificationTokenRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.RefreshTokenRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.UserRepository;
import com.baskaaleksander.nuvine.integration.base.BaseControllerIntegrationTest;
import com.baskaaleksander.nuvine.integration.support.JwtTestUtils;
import com.baskaaleksander.nuvine.integration.support.TestDataBuilder;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AuthControllerIT extends BaseControllerIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private TestDataBuilder testData;

    @BeforeEach
    void setUp() {
        testData.cleanUp();
    }

    @Test
    void register_createsUserAndPublishesEvent() throws Exception {
        BlockingQueue<ConsumerRecord<String, UserRegisteredEvent>> queue =
                createConsumer("user-registered-topic-test", UserRegisteredEvent.class);

        UUID userId = UUID.randomUUID();
        RegisterRequest request = new RegisterRequest("Jane", "Doe", "jane@example.com", "Password123!");

        wireMockStubs.stubUserSearchEmpty();
        wireMockStubs.stubCreateUser(userId);
        wireMockStubs.stubResetPassword(userId.toString());
        wireMockStubs.stubRoleLookup("ROLE_USER");
        wireMockStubs.stubRoleMappingAdd(userId);
        wireMockStubs.stubRoleMappingGet(userId, List.of("ROLE_USER"));

        ResponseEntity<UserResponse> response =
                restTemplate.postForEntity("/api/v1/auth/register", request, UserResponse.class);

        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(userId, response.getBody().id());
        assertEquals(request.email(), response.getBody().email());

        User savedUser = userRepository.findById(userId).orElseThrow();
        assertEquals(request.email(), savedUser.getEmail());

        var verificationTokens = emailVerificationTokenRepository.findAll();
        assertEquals(1, verificationTokens.size());

        UserRegisteredEvent event = awaitMessage(queue, 20, TimeUnit.SECONDS);
        assertNotNull(event);
        assertEquals(request.email(), event.email());
        assertEquals(userId.toString(), event.userId());
        assertEquals(verificationTokens.getFirst().getToken(), event.emailVerificationToken());
    }

    @Test
    void login_storesRefreshToken() {
        UUID userId = UUID.randomUUID();
        User user = testData.createUser(userId, "login@example.com");

        wireMockStubs.stubPasswordToken("access-token", "refresh-token");

        ResponseEntity<TokenResponse> response =
                restTemplate.postForEntity("/api/v1/auth/login",
                        new LoginRequest(user.getEmail(), "Password123!"),
                        TokenResponse.class);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("access-token", response.getBody().accessToken());

        String cookieHeader = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertNotNull(cookieHeader);
        assertTrue(cookieHeader.contains("refresh_token=refresh-token"));

        var savedToken = refreshTokenRepository.findByToken("refresh-token").orElseThrow();
        assertEquals(userId, savedToken.getUser().getId());
    }

    @Test
    void refresh_rotatesRefreshToken() {
        UUID userId = UUID.randomUUID();
        User user = testData.createUser(userId, "refresh@example.com");
        testData.createRefreshToken(user, "refresh-old", Instant.now().plusSeconds(3600), false);

        wireMockStubs.stubRefreshToken("refresh-old", "access-new", "refresh-new");

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "refresh_token=refresh-old");

        ResponseEntity<TokenResponse> response = restTemplate.exchange(
                "/api/v1/auth/refresh",
                HttpMethod.POST,
                new HttpEntity<>(null, headers),
                TokenResponse.class
        );

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("access-new", response.getBody().accessToken());

        String cookieHeader = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertNotNull(cookieHeader);
        assertTrue(cookieHeader.contains("refresh_token=refresh-new"));

        var oldToken = refreshTokenRepository.findByToken("refresh-old").orElseThrow();
        assertTrue(oldToken.getRevoked());

        assertTrue(refreshTokenRepository.findByToken("refresh-new").isPresent());
    }

    @Test
    void getMe_returnsUser() {
        UUID userId = UUID.randomUUID();
        User user = testData.createUser(userId, "me@example.com", "Jane", "Doe", true, false);

        JwtTestUtils jwtUtils = jwtTestUtils();
        String jwt = jwtUtils.generateJwt(userId, user.getEmail(), List.of("ROLE_USER"), true);

        ResponseEntity<MeResponse> response = restTemplate.exchange(
                "/api/v1/auth/me",
                HttpMethod.GET,
                new HttpEntity<>(null, authHeaders(jwt)),
                MeResponse.class
        );

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(userId, response.getBody().id());
        assertEquals(user.getEmail(), response.getBody().email());
        assertEquals("Jane", response.getBody().firstName());
        assertEquals("Doe", response.getBody().lastName());
        assertTrue(response.getBody().roles().contains("ROLE_USER"));
        assertTrue(response.getBody().emailVerified());
    }

    @Test
    void updateMe_updatesUser() {
        UUID userId = UUID.randomUUID();
        User user = testData.createUser(userId, "update@example.com", "Old", "Name", false, false);

        JwtTestUtils jwtUtils = jwtTestUtils();
        String jwt = jwtUtils.generateJwt(userId, user.getEmail());

        UpdateMeRequest request = new UpdateMeRequest("NewFirst", "NewLast");

        ResponseEntity<MeResponse> response = restTemplate.exchange(
                "/api/v1/auth/me",
                HttpMethod.PATCH,
                new HttpEntity<>(request, authHeaders(jwt)),
                MeResponse.class
        );

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("NewFirst", response.getBody().firstName());
        assertEquals("NewLast", response.getBody().lastName());

        User updated = userRepository.findById(userId).orElseThrow();
        assertEquals("NewFirst", updated.getFirstName());
        assertEquals("NewLast", updated.getLastName());
    }

    @Test
    void logout_revokesToken() {
        UUID userId = UUID.randomUUID();
        User user = testData.createUser(userId, "logout@example.com");
        testData.createRefreshToken(user, "refresh-logout", Instant.now().plusSeconds(3600), false);

        JwtTestUtils jwtUtils = jwtTestUtils();
        String jwt = jwtUtils.generateJwt(userId, user.getEmail());

        HttpHeaders headers = authHeaders(jwt);
        headers.add(HttpHeaders.COOKIE, "refresh_token=refresh-logout");

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/auth/logout",
                HttpMethod.POST,
                new HttpEntity<>(null, headers),
                Void.class
        );

        assertEquals(200, response.getStatusCode().value());
        assertTrue(refreshTokenRepository.findByToken("refresh-logout").orElseThrow().getRevoked());
    }

    @Test
    void logoutAll_revokesAllTokens() {
        UUID userId = UUID.randomUUID();
        User user = testData.createUser(userId, "logout-all@example.com");
        testData.createRefreshToken(user, "refresh-one", Instant.now().plusSeconds(3600), false);
        testData.createRefreshToken(user, "refresh-two", Instant.now().plusSeconds(3600), false);

        JwtTestUtils jwtUtils = jwtTestUtils();
        String jwt = jwtUtils.generateJwt(userId, user.getEmail());

        HttpHeaders headers = authHeaders(jwt);
        headers.add(HttpHeaders.COOKIE, "refresh_token=refresh-one");

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/auth/logout-all",
                HttpMethod.POST,
                new HttpEntity<>(null, headers),
                Void.class
        );

        assertEquals(200, response.getStatusCode().value());
        assertTrue(refreshTokenRepository.findByToken("refresh-one").orElseThrow().getRevoked());
        assertTrue(refreshTokenRepository.findByToken("refresh-two").orElseThrow().getRevoked());
    }

    private JwtTestUtils jwtTestUtils() {
        String issuer = "http://localhost:" + wireMockServer.port() + "/realms/nuvine";
        return new JwtTestUtils(issuer);
    }
}
