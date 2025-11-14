package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.domain.exception.EmailVerificationTokenNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.InvalidEmailVerificationTokenException;
import com.baskaaleksander.nuvine.domain.exception.UserNotFoundException;
import com.baskaaleksander.nuvine.domain.model.EmailVerificationToken;
import com.baskaaleksander.nuvine.domain.model.User;
import com.baskaaleksander.nuvine.infrastructure.config.KeycloakClientProvider;
import com.baskaaleksander.nuvine.infrastructure.messaging.EmailVerificationEventProducer;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmailVerificationEvent;
import com.baskaaleksander.nuvine.infrastructure.repository.EmailVerificationTokenRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final EmailVerificationTokenRepository repository;
    private final UserRepository userRepository;
    private final EmailVerificationEventProducer eventProducer;
    private final KeycloakClientProvider keycloakClientProvider;
    @Value("${keycloak.realm}")
    private String realm;

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

    @Transactional
    public void verifyEmail(String token) {
        var tokenEntity = repository.findByToken(token)
                .orElseThrow(() -> new EmailVerificationTokenNotFoundException("Token not found"));

        if (tokenEntity.getExpiresAt().isBefore(Instant.now()) || tokenEntity.getUsedAt() != null) {
            throw new InvalidEmailVerificationTokenException("Invalid token");
        }

        updateKeycloakUserEmailVerified(tokenEntity.getUser().getId().toString());
        userRepository.updateEmailVerified(tokenEntity.getUser().getEmail(), true);
        tokenEntity.setUsedAt(Instant.now());
        repository.save(tokenEntity);
    }

    private void updateKeycloakUserEmailVerified(String userId) {
        Keycloak keycloak = keycloakClientProvider.getInstance();

        UserResource userResource = keycloak.realm(realm).users().get(userId);
        UserRepresentation user = userResource.toRepresentation();

        user.setEmailVerified(true);

        userResource.update(user);
    }
}
