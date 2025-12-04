package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.util.MaskingUtil;
import com.baskaaleksander.nuvine.domain.exception.*;
import com.baskaaleksander.nuvine.domain.model.EmailVerificationToken;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final EmailVerificationTokenRepository repository;
    private final UserRepository userRepository;
    private final EmailVerificationEventProducer eventProducer;
    private final KeycloakClientProvider keycloakClientProvider;
    private final EmailVerificationTokenGenerationService tokenGenerationService;
    @Value("${keycloak.realm}")
    private String realm;


    public void requestVerificationLink(String email) {
        var user = userRepository.findByEmail(email);
        log.info("REQUEST_EMAIL_VERIFY START email={}", MaskingUtil.maskEmail(email));

        if (user.isPresent()) {
            EmailVerificationToken verificationToken = tokenGenerationService.createToken(user.get());
            eventProducer.sendEmailVerificationEvent(
                    new EmailVerificationEvent(
                            email,
                            verificationToken.getToken(),
                            user.get().getId().toString()
                    )
            );
            log.info("REQUEST_EMAIL_VERIFY SUCCESS email={}", MaskingUtil.maskEmail(email));
        } else {
            log.info("REQUEST_EMAIL_VERIFY FAILED reason=user_not_found email={}", MaskingUtil.maskEmail(email));
        }
    }

    @Transactional
    public void verifyEmail(String token) {
        log.info("EMAIL_VERIFY START token={}", MaskingUtil.maskToken(token));
        var tokenEntity = repository.findByToken(token)
                .orElseThrow(() -> new EmailVerificationTokenNotFoundException("Token not found"));

        log.info("EMAIL_VERIFY TOKEN FOUND userId={}", tokenEntity.getUser().getId());

        if (tokenEntity.getExpiresAt().isBefore(Instant.now()) || tokenEntity.getUsedAt() != null) {
            log.info("EMAIL_VERIFY FAILED reason=expired userId={}", tokenEntity.getUser().getId());
            throw new InvalidEmailVerificationTokenException("Invalid token");
        }

        log.info("EMAIL_VERIFY TOKEN VALID userId={}", tokenEntity.getUser().getId());

        updateKeycloakUserEmailVerified(tokenEntity.getUser().getId().toString());
        userRepository.updateEmailVerified(tokenEntity.getUser().getEmail(), true);
        tokenEntity.setUsedAt(Instant.now());

        repository.save(tokenEntity);
        log.info("EMAIL_VERIFY SUCCESS userId={}", tokenEntity.getUser().getId());
    }

    private void updateKeycloakUserEmailVerified(String userId) {
        Keycloak keycloak = keycloakClientProvider.getInstance();

        log.info("KEYCLOAK_EMAIL_VERIFY START userId={}", userId);
        UserResource userResource = keycloak.realm(realm).users().get(userId);
        UserRepresentation user = userResource.toRepresentation();

        user.setEmailVerified(true);

        userResource.update(user);
        log.info("KEYCLOAK_EMAIL_VERIFY SUCCESS userId={}", userId);
    }

    @Transactional
    public void changeEmail(String oldEmail, String newEmail, String password) {
        log.info("EMAIL_CHANGE START oldEmail={}", MaskingUtil.maskEmail(oldEmail));

        var user = userRepository.findByEmail(oldEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        var existing = userRepository.findByEmail(newEmail);

        if (existing.isPresent()) {
            log.info("EMAIL_CHANGE FAILED reason=email_taken newEmail={}", MaskingUtil.maskEmail(newEmail));
            throw new UserConflictException("User with email " + newEmail + " already exists");
        }

        if (!keycloakClientProvider.verifyPassword(oldEmail, password)) {
            log.info("EMAIL_CHANGE FAILED reason=invalid_password oldEmail={}", MaskingUtil.maskEmail(oldEmail));
            throw new InvalidCredentialsException("Invalid password");
        }

        EmailVerificationToken verificationToken = tokenGenerationService.createToken(user);

        updateKeycloakUserEmail(user.getId().toString(), newEmail);

        userRepository.updateEmail(user.getId(), newEmail);
        userRepository.updateEmailVerifiedByUserId(user.getId(), false);

        eventProducer.sendEmailVerificationEvent(
                new EmailVerificationEvent(
                        newEmail,
                        verificationToken.getToken(),
                        user.getId().toString()
                )
        );

        log.info("EMAIL_CHANGE SUCCESS userId={} oldEmail={} newEmail={}",
                user.getId(),
                MaskingUtil.maskEmail(oldEmail),
                MaskingUtil.maskEmail(newEmail)
        );
    }

    private void updateKeycloakUserEmail(String userId, String newEmail) {
        Keycloak keycloak = keycloakClientProvider.getInstance();

        log.info("KEYCLOAK_EMAIL_CHANGE START userId={}", userId);
        UserResource userResource = keycloak.realm(realm).users().get(userId);
        UserRepresentation user = userResource.toRepresentation();

        user.setEmailVerified(false);
        user.setUsername(newEmail);
        user.setEmail(newEmail);

        userResource.update(user);
        log.info("KEYCLOAK_EMAIL_CHANGE SUCCESS userId={}", userId);
    }
}
