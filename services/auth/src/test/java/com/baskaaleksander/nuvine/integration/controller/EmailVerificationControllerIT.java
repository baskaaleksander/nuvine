package com.baskaaleksander.nuvine.integration.controller;

import com.baskaaleksander.nuvine.application.dto.EmailChangeRequest;
import com.baskaaleksander.nuvine.application.dto.EmailVerificationRequest;
import com.baskaaleksander.nuvine.domain.model.EmailVerificationToken;
import com.baskaaleksander.nuvine.domain.model.User;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmailVerificationEvent;
import com.baskaaleksander.nuvine.infrastructure.repository.EmailVerificationTokenRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.UserRepository;
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

class EmailVerificationControllerIT extends BaseControllerIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private TestDataBuilder testData;

    @BeforeEach
    void setUp() {
        testData.cleanUp();
    }

    @Test
    void requestVerification_createsTokenAndPublishesEvent() throws Exception {
        BlockingQueue<ConsumerRecord<String, EmailVerificationEvent>> queue =
                createConsumer("email-verification-topic-test", EmailVerificationEvent.class);

        UUID userId = UUID.randomUUID();
        User user = testData.createUser(userId, "verify@example.com");

        JwtTestUtils jwtUtils = jwtTestUtils();
        String jwt = jwtUtils.generateJwt(userId, user.getEmail());

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/auth/email/request",
                HttpMethod.POST,
                new HttpEntity<>(null, authHeaders(jwt)),
                Void.class
        );

        assertEquals(200, response.getStatusCode().value());

        EmailVerificationToken token = emailVerificationTokenRepository.findAll().getFirst();
        EmailVerificationEvent event = awaitMessage(queue, 20, TimeUnit.SECONDS);
        assertNotNull(event);
        assertEquals(user.getEmail(), event.email());
        assertEquals(userId.toString(), event.userId());
        assertEquals(token.getToken(), event.token());
    }

    @Test
    void verifyEmail_marksTokenUsedAndUpdatesUser() {
        UUID userId = UUID.randomUUID();
        User user = testData.createUser(userId, "verify-token@example.com", "Jane", "Doe", false, false);
        EmailVerificationToken token = testData.createEmailVerificationToken(
                user,
                "verify-token",
                Instant.now().plusSeconds(3600)
        );

        wireMockStubs.stubUserGet(userId, user.getEmail(), user.getFirstName(), user.getLastName(), false);
        wireMockStubs.stubUserUpdate(userId);

        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/api/v1/auth/email/verify",
                new EmailVerificationRequest(token.getToken()),
                Void.class
        );

        assertEquals(200, response.getStatusCode().value());

        User updated = userRepository.findById(userId).orElseThrow();
        assertTrue(updated.isEmailVerified());

        EmailVerificationToken updatedToken = emailVerificationTokenRepository.findByToken(token.getToken()).orElseThrow();
        assertNotNull(updatedToken.getUsedAt());
    }

    @Test
    void changeEmail_updatesUserAndPublishesEvent() throws Exception {
        BlockingQueue<ConsumerRecord<String, EmailVerificationEvent>> queue =
                createConsumer("email-verification-topic-test", EmailVerificationEvent.class);

        UUID userId = UUID.randomUUID();
        User user = testData.createUser(userId, "old@example.com", "Jane", "Doe", true, true);

        wireMockStubs.stubPasswordToken("access-token", "refresh-token");
        wireMockStubs.stubUserGet(userId, user.getEmail(), user.getFirstName(), user.getLastName(), true);
        wireMockStubs.stubUserUpdate(userId);

        JwtTestUtils jwtUtils = jwtTestUtils();
        String jwt = jwtUtils.generateJwt(userId, user.getEmail());

        EmailChangeRequest request = new EmailChangeRequest("Password123!", "new@example.com");

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/auth/email/change",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders(jwt)),
                Void.class
        );

        assertEquals(200, response.getStatusCode().value());

        User updated = userRepository.findById(userId).orElseThrow();
        assertEquals("new@example.com", updated.getEmail());
        assertFalse(updated.isEmailVerified());

        EmailVerificationEvent event = awaitMessage(queue, 20, TimeUnit.SECONDS);
        assertNotNull(event);
        assertEquals("new@example.com", event.email());
        assertEquals(userId.toString(), event.userId());
    }

    private JwtTestUtils jwtTestUtils() {
        String issuer = "http://localhost:" + wireMockServer.port() + "/realms/nuvine";
        return new JwtTestUtils(issuer);
    }
}
