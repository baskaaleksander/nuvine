package com.baskaaleksander.nuvine.integration.support;

import com.baskaaleksander.nuvine.domain.model.EmailVerificationToken;
import com.baskaaleksander.nuvine.domain.model.PasswordResetToken;
import com.baskaaleksander.nuvine.domain.model.RefreshToken;
import com.baskaaleksander.nuvine.domain.model.User;
import com.baskaaleksander.nuvine.infrastructure.repository.EmailVerificationTokenRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.PasswordResetTokenRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.RefreshTokenRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class TestDataBuilder {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public TestDataBuilder(UserRepository userRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           EmailVerificationTokenRepository emailVerificationTokenRepository,
                           PasswordResetTokenRepository passwordResetTokenRepository) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    public User createUser(UUID id, String email, String firstName, String lastName,
                           boolean emailVerified, boolean onboardingCompleted) {
        User user = User.builder()
                .id(id)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .emailVerified(emailVerified)
                .onboardingCompleted(onboardingCompleted)
                .build();
        return userRepository.save(user);
    }

    public User createUser(UUID id, String email) {
        return createUser(id, email, "Test", "User", false, false);
    }

    public RefreshToken createRefreshToken(User user, String token, Instant expiresAt, boolean revoked) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiresAt(expiresAt)
                .revoked(revoked)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    public EmailVerificationToken createEmailVerificationToken(User user, String token, Instant expiresAt) {
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .user(user)
                .token(token)
                .expiresAt(expiresAt)
                .build();
        return emailVerificationTokenRepository.save(verificationToken);
    }

    public PasswordResetToken createPasswordResetToken(User user, String token, Instant expiresAt) {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(expiresAt)
                .build();
        return passwordResetTokenRepository.save(resetToken);
    }

    public void cleanUp() {
        passwordResetTokenRepository.deleteAll();
        emailVerificationTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }
}
