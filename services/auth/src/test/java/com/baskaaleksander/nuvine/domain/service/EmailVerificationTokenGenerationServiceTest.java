package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.domain.model.EmailVerificationToken;
import com.baskaaleksander.nuvine.domain.model.User;
import com.baskaaleksander.nuvine.infrastructure.repository.EmailVerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationTokenGenerationServiceTest {

    @Mock
    private EmailVerificationTokenRepository repository;

    @InjectMocks
    private EmailVerificationTokenGenerationService tokenGenerationService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .build();
    }

    @Test
    void createTokenPersistsTokenWithExpectedFields() {
        when(repository.save(any(EmailVerificationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Instant beforeCall = Instant.now();

        EmailVerificationToken created = tokenGenerationService.createToken(user);

        ArgumentCaptor<EmailVerificationToken> captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(repository).save(captor.capture());
        EmailVerificationToken saved = captor.getValue();
        assertEquals(user, saved.getUser());
        assertNotNull(saved.getToken());
        long secondsUntilExpire = Duration.between(beforeCall, saved.getExpiresAt()).getSeconds();
        assertEquals(saved, created);
        // allow a few seconds tolerance around 7 days
        long expected = Duration.ofDays(7).getSeconds();
        assertTrue(Math.abs(secondsUntilExpire - expected) <= 5,
                "expiresAt should be roughly 7 days ahead");
    }

    @Test
    void createTokenPropagatesRepositoryErrors() {
        doThrow(new RuntimeException("DB error")).when(repository).save(any());

        assertThrows(RuntimeException.class, () -> tokenGenerationService.createToken(user));
    }
}
