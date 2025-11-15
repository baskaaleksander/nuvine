package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.util.MaskingUtil;
import com.baskaaleksander.nuvine.domain.model.PasswordResetToken;
import com.baskaaleksander.nuvine.domain.model.User;
import com.baskaaleksander.nuvine.infrastructure.messaging.PasswordResetEventProducer;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.PasswordResetEvent;
import com.baskaaleksander.nuvine.infrastructure.repository.PasswordResetTokenRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private static final long EXPIRATION_TIME = 24 * 3600;

    public void requestPasswordReset(String email) {
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
        } else {
            log.info("User not found email={}", MaskingUtil.maskEmail(email));
        }
    }

    private PasswordResetToken createToken(User user) {
        log.info("Creating password reset token userId={}", user.getId());
        UUID resetToken = UUID.randomUUID();
        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .token(resetToken.toString())
                .expiresAt(Instant.now().plusSeconds(EXPIRATION_TIME))
                .build();

        log.info("Password reset token created userId={}", user.getId());

        return repository.save(token);
    }
}
