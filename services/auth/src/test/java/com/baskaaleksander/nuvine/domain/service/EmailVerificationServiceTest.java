package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.domain.exception.EmailVerificationTokenNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.InvalidCredentialsException;
import com.baskaaleksander.nuvine.domain.exception.InvalidEmailVerificationTokenException;
import com.baskaaleksander.nuvine.domain.exception.UserConflictException;
import com.baskaaleksander.nuvine.domain.exception.UserNotFoundException;
import com.baskaaleksander.nuvine.domain.model.EmailVerificationToken;
import com.baskaaleksander.nuvine.domain.model.User;
import com.baskaaleksander.nuvine.infrastructure.config.KeycloakClientProvider;
import com.baskaaleksander.nuvine.infrastructure.messaging.EmailVerificationEventProducer;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmailVerificationEvent;
import com.baskaaleksander.nuvine.infrastructure.repository.EmailVerificationTokenRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationTokenRepository tokenRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailVerificationEventProducer eventProducer;
    @Mock
    private KeycloakClientProvider keycloakClientProvider;
    @Mock
    private EmailVerificationTokenGenerationService tokenGenerationService;
    @Mock
    private Keycloak keycloak;
    @Mock
    private RealmResource realmResource;
    @Mock
    private UsersResource usersResource;
    @Mock
    private UserResource userResource;
    @Mock
    private UserRepresentation userRepresentation;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    private User user;
    private EmailVerificationToken token;
    private String tokenValue;
    private String email;
    private static final String REALM = "test-realm";
    private String newEmail;
    private String password;

    @BeforeEach
    void setUp() {
        email = "user@example.com";
        newEmail = "new@example.com";
        password = "Password#123";
        user = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .build();
        tokenValue = UUID.randomUUID().toString();
        token = EmailVerificationToken.builder()
                .token(tokenValue)
                .user(user)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        ReflectionTestUtils.setField(emailVerificationService, "realm", REALM);
    }

    @Test
    void requestVerificationLinkDoesNothingWhenUserMissing() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        emailVerificationService.requestVerificationLink(email);

        verify(userRepository).findByEmail(email);
        verifyNoInteractions(tokenGenerationService, eventProducer);
    }

    @Test
    void requestVerificationLinkGeneratesTokenAndSendsEvent() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(tokenGenerationService.createToken(user)).thenReturn(token);

        emailVerificationService.requestVerificationLink(email);

        verify(tokenGenerationService).createToken(user);
        ArgumentCaptor<EmailVerificationEvent> eventCaptor = ArgumentCaptor.forClass(EmailVerificationEvent.class);
        verify(eventProducer).sendEmailVerificationEvent(eventCaptor.capture());
        EmailVerificationEvent event = eventCaptor.getValue();
        assertEquals(email, event.email());
        assertEquals(token.toString(), event.token());
        assertEquals(user.getId().toString(), event.userId());
    }

    @Test
    void verifyEmailThrowsWhenTokenNotFound() {
        when(tokenRepository.findByToken(tokenValue)).thenReturn(Optional.empty());

        assertThrows(EmailVerificationTokenNotFoundException.class,
                () -> emailVerificationService.verifyEmail(tokenValue));
    }

    @Test
    void verifyEmailThrowsWhenTokenExpiredOrUsed() {
        token.setExpiresAt(Instant.now().minusSeconds(5));
        when(tokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(token));

        assertThrows(InvalidEmailVerificationTokenException.class,
                () -> emailVerificationService.verifyEmail(tokenValue));

        token.setExpiresAt(Instant.now().plusSeconds(3600));
        token.setUsedAt(Instant.now());
        when(tokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(token));

        assertThrows(InvalidEmailVerificationTokenException.class,
                () -> emailVerificationService.verifyEmail(tokenValue));
    }

    @Test
    void verifyEmailUpdatesKeycloakAndMarksTokenUsed() {
        when(tokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(token));
        when(keycloakClientProvider.getInstance()).thenReturn(keycloak);
        when(keycloak.realm(REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(user.getId().toString())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(userRepresentation);

        emailVerificationService.verifyEmail(tokenValue);

        verify(userRepresentation).setEmailVerified(true);
        verify(userResource).update(userRepresentation);
        verify(userRepository).updateEmailVerified(user.getEmail(), true);
        assertNotNull(token.getUsedAt());
        verify(tokenRepository).save(token);
    }

    @Test
    void verifyEmailThrowsWhenKeycloakUpdateFails() {
        when(tokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(token));
        when(keycloakClientProvider.getInstance()).thenReturn(keycloak);
        when(keycloak.realm(REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(user.getId().toString())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(userRepresentation);
        doThrow(new RuntimeException("Keycloak error")).when(userResource).update(userRepresentation);

        assertThrows(RuntimeException.class, () -> emailVerificationService.verifyEmail(tokenValue));

        verify(userRepository, never()).updateEmailVerified(anyString(), anyBoolean());
        verify(tokenRepository, never()).save(token);
    }

    @Test
    void changeEmailThrowsWhenUserNotFound() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> emailVerificationService.changeEmail(email, newEmail, password));
    }

    @Test
    void changeEmailThrowsWhenNewEmailAlreadyExists() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(newEmail)).thenReturn(Optional.of(User.builder().id(UUID.randomUUID()).email(newEmail).build()));

        assertThrows(UserConflictException.class,
                () -> emailVerificationService.changeEmail(email, newEmail, password));
    }

    @Test
    void changeEmailThrowsWhenPasswordInvalid() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(newEmail)).thenReturn(Optional.empty());
        when(keycloakClientProvider.verifyPassword(email, password)).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> emailVerificationService.changeEmail(email, newEmail, password));

        verify(tokenGenerationService, never()).createToken(user);
        verifyNoInteractions(eventProducer);
    }

    @Test
    void changeEmailUpdatesKeycloakAndSendsEvent() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(newEmail)).thenReturn(Optional.empty());
        when(keycloakClientProvider.verifyPassword(email, password)).thenReturn(true);
        when(tokenGenerationService.createToken(user)).thenReturn(token);
        when(keycloakClientProvider.getInstance()).thenReturn(keycloak);
        when(keycloak.realm(REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(user.getId().toString())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(userRepresentation);

        emailVerificationService.changeEmail(email, newEmail, password);

        verify(tokenGenerationService).createToken(user);
        verify(userRepresentation).setEmailVerified(false);
        verify(userRepresentation).setUsername(newEmail);
        verify(userRepresentation).setEmail(newEmail);
        verify(userResource).update(userRepresentation);
        verify(userRepository).updateEmail(user.getId(), newEmail);
        verify(userRepository).updateEmailVerifiedByUserId(user.getId(), false);
        ArgumentCaptor<EmailVerificationEvent> eventCaptor = ArgumentCaptor.forClass(EmailVerificationEvent.class);
        verify(eventProducer).sendEmailVerificationEvent(eventCaptor.capture());
        EmailVerificationEvent event = eventCaptor.getValue();
        assertEquals(newEmail, event.email());
        assertEquals(token.getToken(), event.token());
        assertEquals(user.getId().toString(), event.userId());
    }

    @Test
    void changeEmailThrowsWhenKeycloakUpdateFails() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(newEmail)).thenReturn(Optional.empty());
        when(keycloakClientProvider.verifyPassword(email, password)).thenReturn(true);
        when(tokenGenerationService.createToken(user)).thenReturn(token);
        when(keycloakClientProvider.getInstance()).thenReturn(keycloak);
        when(keycloak.realm(REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(user.getId().toString())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(userRepresentation);
        doThrow(new RuntimeException("Keycloak error")).when(userResource).update(userRepresentation);

        assertThrows(RuntimeException.class,
                () -> emailVerificationService.changeEmail(email, newEmail, password));

        verify(userRepository, never()).updateEmail(user.getId(), newEmail);
        verify(userRepository, never()).updateEmailVerifiedByUserId(user.getId(), false);
        verify(eventProducer, never()).sendEmailVerificationEvent(any());
    }
}
