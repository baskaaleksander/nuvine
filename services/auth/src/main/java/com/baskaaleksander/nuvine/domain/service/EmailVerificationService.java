package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.domain.exception.UserNotFoundException;
import com.baskaaleksander.nuvine.domain.model.EmailVerificationToken;
import com.baskaaleksander.nuvine.domain.model.User;
import com.baskaaleksander.nuvine.infrastructure.messaging.EmailVerificationEventProducer;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmailVerificationEvent;
import com.baskaaleksander.nuvine.infrastructure.repository.EmailVerificationTokenRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final EmailVerificationTokenRepository repository;
    private final UserRepository userRepository;
    private final EmailVerificationEventProducer eventProducer;

    private static final long EXPIRATION_TIME = 7 * 24 * 3600;

    public void requestVerificationLink(String email) {
        var user = userRepository.findByEmail(email);

        if (user.isPresent()) {
            UUID verificationToken = UUID.randomUUID();
            EmailVerificationToken token = EmailVerificationToken.builder()
                    .user(user.get())
                    .token(verificationToken.toString())
                    .expiresAt(Instant.now().plusSeconds(EXPIRATION_TIME))
                    .build();

            repository.save(token);
            eventProducer.sendEmailVerificationEvent(
                    new EmailVerificationEvent(
                            email,
                            verificationToken.toString(),
                            user.get().getId().toString()
                    )
            );
        }
    }
}
