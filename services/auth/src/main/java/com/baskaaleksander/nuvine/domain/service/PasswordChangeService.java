package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.PasswordChangeRequest;
import com.baskaaleksander.nuvine.application.dto.PasswordResetRequest;
import com.baskaaleksander.nuvine.application.util.MaskingUtil;
import com.baskaaleksander.nuvine.domain.exception.ExternalIdentityProviderException;
import com.baskaaleksander.nuvine.domain.model.PasswordResetToken;
import com.baskaaleksander.nuvine.domain.model.User;
import com.baskaaleksander.nuvine.infrastructure.config.KeycloakClientProvider;
import com.baskaaleksander.nuvine.infrastructure.messaging.PasswordResetEventProducer;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.PasswordResetEvent;
import com.baskaaleksander.nuvine.infrastructure.repository.PasswordResetTokenRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PasswordChangeService {

    private final PasswordResetTokenRepository repository;
    private final UserRepository userRepository;
    private final PasswordResetEventProducer eventProducer;
    private final KeycloakClientProvider clientProvider;
    private static final long EXPIRATION_TIME = 24 * 3600;

    @Value("${keycloak.realm}")
    private String realm;

    public void changePassword(PasswordChangeRequest request, String email) {
        log.info("CHANGE_PASSWORD START email={}", MaskingUtil.maskEmail(email));
        if (!clientProvider.verifyPassword(email, request.oldPassword())) throw new RuntimeException();

        if (!request.newPassword().equalsIgnoreCase(request.confirmNewPassword())) {
            log.info("CHANGE_PASSWORD FAILED reason=password_mismatch email={}", MaskingUtil.maskEmail(email));
            throw new RuntimeException();
        }

        try {
            updateKeycloakPassword(email, request.newPassword());
            log.info("CHANGE_PASSWORD SUCCESS email={}", MaskingUtil.maskEmail(email));
        } catch (Exception ex) {
            log.error("CHANGE_PASSWORD FAILED reason=keycloak_error email={}", MaskingUtil.maskEmail(email));
            throw new ExternalIdentityProviderException("Failed to update password in Keycloak");
        }
    }

    public void resetPassword(PasswordResetRequest request) {
        log.info("RESET_PASSWORD START token={}", MaskingUtil.maskToken(request.token()));
        checkToken(request.token());

        if (!request.password().equalsIgnoreCase(request.confirmPassword())) throw new RuntimeException();

        var token = repository.findByToken(request.token())
                .orElseThrow(() -> new RuntimeException("Token not found"));

        try {
            updateKeycloakPassword(token.getUser().getId().toString(), request.password());
        } catch (Exception ex) {
            log.error("RESET_PASSWORD FAILED reason=keycloak_error token={}", MaskingUtil.maskToken(request.token()));
            throw new ExternalIdentityProviderException("Failed to update password in Keycloak");
        } finally {
            token.setUsedAt(Instant.now());
            repository.save(token);
            log.info("RESET_PASSWORD SUCCESS token={}", MaskingUtil.maskToken(request.token()));
        }
    }

    public void checkToken(String token) {
        log.info("CHECK_TOKEN START token={}", MaskingUtil.maskToken(token));
        var refreshToken = repository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        if (refreshToken.getExpiresAt().isBefore(Instant.now()) || refreshToken.getUsedAt() != null) {
            log.info("CHECK_TOKEN FAILED reason=expired_or_used token={}", MaskingUtil.maskToken(token));
            throw new RuntimeException("Token expired or already used");
        }
    }

    public void requestPasswordReset(String email) {
        log.info("REQUEST_PASSWORD_RESET START email={}", MaskingUtil.maskEmail(email));
        var user = userRepository.findByEmail(email);

        if (user.isPresent()) {
            PasswordResetToken token = createToken(user.get());
            eventProducer.sendPasswordResetEvent(
                    new PasswordResetEvent(
                            email,
                            token.getToken(),
                            user.get().getId().toString()
                    )

            );
            log.info("REQUEST_PASSWORD_RESET SUCCESS email={}", MaskingUtil.maskEmail(email));
        } else {
            log.info("REQUEST_PASSWORD_RESET FAILED reason=user_not_found email={}", MaskingUtil.maskEmail(email));
        }
    }

    private PasswordResetToken createToken(User user) {
        log.info("CREATE_PASSWORD_RESET_TOKEN START userId={}", user.getId());
        UUID resetToken = UUID.randomUUID();
        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .token(resetToken.toString())
                .expiresAt(Instant.now().plusSeconds(EXPIRATION_TIME))
                .build();

        log.info("CREATE_PASSWORD_RESET_TOKEN SUCCESS userId={}", user.getId());

        return repository.save(token);
    }

    private void updateKeycloakPassword(String userId, String newPassword) {
        Keycloak keycloak = clientProvider.getInstance();

        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(newPassword);
        cred.setTemporary(false);

        keycloak
                .realm(realm)
                .users()
                .get(userId)
                .resetPassword(cred);
    }
}
