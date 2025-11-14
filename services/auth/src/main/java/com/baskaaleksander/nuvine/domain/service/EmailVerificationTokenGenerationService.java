package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.domain.model.EmailVerificationToken;
import com.baskaaleksander.nuvine.domain.model.User;
import com.baskaaleksander.nuvine.infrastructure.repository.EmailVerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmailVerificationTokenGenerationService {

    private static final long EXPIRATION_TIME = 7 * 24 * 3600;
    private final EmailVerificationTokenRepository repository;

    public EmailVerificationToken createToken(User user) {
        log.info("Creating email verification token userId={}", user.getId());
        UUID verificationToken = UUID.randomUUID();
        EmailVerificationToken token = EmailVerificationToken.builder()
                .user(user)
                .token(verificationToken.toString())
                .expiresAt(Instant.now().plusSeconds(EXPIRATION_TIME))
                .build();

        log.info("Email verification token created userId={}", user.getId());

        return repository.save(token);
    }
}
