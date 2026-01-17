package com.baskaaleksander.nuvine.integration.controller;

import com.baskaaleksander.nuvine.application.dto.CheckTokenRequest;
import com.baskaaleksander.nuvine.application.dto.ForgotPasswordRequest;
import com.baskaaleksander.nuvine.application.dto.PasswordChangeRequest;
import com.baskaaleksander.nuvine.application.dto.PasswordResetRequest;
import com.baskaaleksander.nuvine.domain.model.PasswordResetToken;
import com.baskaaleksander.nuvine.domain.model.User;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.PasswordResetEvent;
import com.baskaaleksander.nuvine.infrastructure.repository.PasswordResetTokenRepository;
import com.baskaaleksander.nuvine.integration.base.BaseControllerIntegrationTest;
import com.baskaaleksander.nuvine.integration.support.JwtTestUtils;
import com.baskaaleksander.nuvine.integration.support.TestDataBuilder;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class PasswordChangeControllerIT extends BaseControllerIntegrationTest {

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private TestDataBuilder testData;

    @BeforeEach
    void setUp() {
        testData.cleanUp();
    }

    @Test
    void forgotPassword_createsTokenAndPublishesEvent() throws Exception {
        BlockingQueue<ConsumerRecord<String, PasswordResetEvent>> queue =
                createConsumer("password-reset-topic-test", PasswordResetEvent.class);

        UUID userId = UUID.randomUUID();
        User user = testData.createUser(userId, "forgot@example.com");

        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/api/v1/auth/password/forgot",
                new ForgotPasswordRequest(user.getEmail()),
                Void.class
        );

        assertEquals(200, response.getStatusCode().value());

        PasswordResetToken token = passwordResetTokenRepository.findAll().getFirst();
        PasswordResetEvent event = awaitMessage(queue, 20, TimeUnit.SECONDS);
        assertNotNull(event);
        assertEquals(user.getEmail(), event.email());
        assertEquals(userId.toString(), event.userId());
        assertEquals(token.getToken(), event.token());
    }

    @Test
    void resetPassword_marksTokenUsed() {
        UUID userId = UUID.randomUUID();
        User user = testData.createUser(userId, "reset@example.com");
        PasswordResetToken token = testData.createPasswordResetToken(
                user,
                "reset-token",
                Instant.now().plusSeconds(3600)
        );

        wireMockStubs.stubResetPassword(userId.toString());

        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/api/v1/auth/password/reset",
                new PasswordResetRequest(token.getToken(), "Password123!", "Password123!"),
                Void.class
        );

        assertEquals(200, response.getStatusCode().value());

        PasswordResetToken updated = passwordResetTokenRepository.findByToken(token.getToken()).orElseThrow();
        assertNotNull(updated.getUsedAt());
    }

    @Test
    void changePassword_updatesKeycloak() {
        UUID userId = UUID.randomUUID();
        User user = testData.createUser(userId, "change@example.com");

        wireMockStubs.stubPasswordToken("access-token", "refresh-token");
        wireMockStubs.stubResetPassword(user.getEmail());

        JwtTestUtils jwtUtils = jwtTestUtils();
        String jwt = jwtUtils.generateJwt(userId, user.getEmail());

        PasswordChangeRequest request = new PasswordChangeRequest("OldPass1!", "NewPass1!", "NewPass1!");

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/auth/password/change",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders(jwt)),
                Void.class
        );

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void checkToken_returnsOk() {
        UUID userId = UUID.randomUUID();
        User user = testData.createUser(userId, "check@example.com");
        PasswordResetToken token = testData.createPasswordResetToken(
                user,
                "check-token",
                Instant.now().plusSeconds(3600)
        );

        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/api/v1/auth/password/check-token",
                new CheckTokenRequest(token.getToken()),
                Void.class
        );

        assertEquals(200, response.getStatusCode().value());
    }

    private JwtTestUtils jwtTestUtils() {
        String issuer = "http://localhost:" + wireMockServer.port() + "/realms/nuvine";
        return new JwtTestUtils(issuer);
    }
}
