package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.PasswordChangeRequest;
import com.baskaaleksander.nuvine.application.dto.PasswordResetRequest;
import com.baskaaleksander.nuvine.domain.exception.ExternalIdentityProviderException;
import com.baskaaleksander.nuvine.domain.model.PasswordResetToken;
import com.baskaaleksander.nuvine.domain.model.User;
import com.baskaaleksander.nuvine.infrastructure.config.KeycloakClientProvider;
import com.baskaaleksander.nuvine.infrastructure.messaging.PasswordResetEventProducer;
import com.baskaaleksander.nuvine.infrastructure.repository.PasswordResetTokenRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordChangeServiceTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordResetEventProducer eventProducer;
    @Mock
    private KeycloakClientProvider clientProvider;
    @Mock
    private Keycloak keycloak;
    @Mock
    private RealmResource realmResource;
    @Mock
    private UsersResource usersResource;
    @Mock
    private UserResource userResource;

    @InjectMocks
    private PasswordChangeService passwordChangeService;

    private static final String REALM = "test-realm";
    private String email;
    private PasswordChangeRequest request;
    private PasswordResetRequest resetRequest;
    private String resetTokenValue;
    private PasswordResetToken passwordResetToken;
    private User user;

    @BeforeEach
    void setUp() {
        email = "user@example.com";
        request = new PasswordChangeRequest("old-password", "NewPassword#1", "NewPassword#1");
        user = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .build();
        resetTokenValue = UUID.randomUUID().toString();
        passwordResetToken = PasswordResetToken.builder()
                .token(resetTokenValue)
                .user(user)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        resetRequest = new PasswordResetRequest(resetTokenValue, "ResetPassword#1", "ResetPassword#1");
        ReflectionTestUtils.setField(passwordChangeService, "realm", REALM);
    }

    @Test
    void changePasswordThrowsWhenOldPasswordVerificationFails() {
        when(clientProvider.verifyPassword(email, request.oldPassword())).thenReturn(false);

        assertThrows(RuntimeException.class, () -> passwordChangeService.changePassword(request, email));

        verify(clientProvider).verifyPassword(email, request.oldPassword());
        verify(clientProvider, never()).getInstance();
        verifyNoInteractions(keycloak, realmResource, usersResource, userResource);
    }

    @Test
    void changePasswordThrowsWhenNewPasswordsDoNotMatch() {
        PasswordChangeRequest mismatchedRequest = new PasswordChangeRequest(
                "old-password", "NewPassword#1", "different"
        );
        when(clientProvider.verifyPassword(email, mismatchedRequest.oldPassword())).thenReturn(true);

        assertThrows(RuntimeException.class, () -> passwordChangeService.changePassword(mismatchedRequest, email));

        verify(clientProvider).verifyPassword(email, mismatchedRequest.oldPassword());
        verify(clientProvider, never()).getInstance();
        verifyNoInteractions(keycloak, realmResource, usersResource, userResource);
    }

    @Test
    void changePasswordUpdatesKeycloakWhenRequestValid() {
        when(clientProvider.verifyPassword(email, request.oldPassword())).thenReturn(true);
        when(clientProvider.getInstance()).thenReturn(keycloak);
        when(keycloak.realm(REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(email)).thenReturn(userResource);

        passwordChangeService.changePassword(request, email);

        ArgumentCaptor<CredentialRepresentation> credentialCaptor = ArgumentCaptor.forClass(CredentialRepresentation.class);
        verify(userResource).resetPassword(credentialCaptor.capture());
        CredentialRepresentation credential = credentialCaptor.getValue();
        assertEquals(CredentialRepresentation.PASSWORD, credential.getType());
        assertEquals(request.newPassword(), credential.getValue());
    }

    @Test
    void changePasswordThrowsExternalIdentityProviderExceptionWhenKeycloakFails() {
        when(clientProvider.verifyPassword(email, request.oldPassword())).thenReturn(true);
        when(clientProvider.getInstance()).thenReturn(keycloak);
        when(keycloak.realm(REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(email)).thenReturn(userResource);
        doThrow(new RuntimeException("Keycloak error")).when(userResource).resetPassword(any());

        assertThrows(ExternalIdentityProviderException.class,
                () -> passwordChangeService.changePassword(request, email));
    }

    @Test
    void resetPasswordThrowsWhenTokenNotFound() {
        when(tokenRepository.findByToken(resetTokenValue)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> passwordChangeService.resetPassword(resetRequest));

        verify(tokenRepository).findByToken(resetTokenValue);
        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(keycloak, realmResource, usersResource, userResource);
    }

    @Test
    void resetPasswordThrowsWhenPasswordsDoNotMatch() {
        PasswordResetRequest mismatched = new PasswordResetRequest(resetTokenValue, "ResetPassword#1", "wrong");
        when(tokenRepository.findByToken(resetTokenValue)).thenReturn(Optional.of(passwordResetToken));

        assertThrows(RuntimeException.class, () -> passwordChangeService.resetPassword(mismatched));

        verify(tokenRepository).findByToken(resetTokenValue);
        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(keycloak, realmResource, usersResource, userResource);
    }

    @Test
    void resetPasswordUpdatesKeycloakAndMarksTokenUsed() {
        when(tokenRepository.findByToken(resetTokenValue))
                .thenReturn(Optional.of(passwordResetToken), Optional.of(passwordResetToken));
        when(clientProvider.getInstance()).thenReturn(keycloak);
        when(keycloak.realm(REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(user.getId().toString())).thenReturn(userResource);

        passwordChangeService.resetPassword(resetRequest);

        ArgumentCaptor<CredentialRepresentation> credentialCaptor = ArgumentCaptor.forClass(CredentialRepresentation.class);
        verify(userResource).resetPassword(credentialCaptor.capture());
        CredentialRepresentation credential = credentialCaptor.getValue();
        assertEquals(resetRequest.password(), credential.getValue());
        verify(tokenRepository).save(passwordResetToken);
        assertNotNull(passwordResetToken.getUsedAt());
        verify(tokenRepository, times(2)).findByToken(resetTokenValue);
    }

    @Test
    void resetPasswordThrowsExternalIdentityProviderExceptionWhenKeycloakFails() {
        when(tokenRepository.findByToken(resetTokenValue))
                .thenReturn(Optional.of(passwordResetToken), Optional.of(passwordResetToken));
        when(clientProvider.getInstance()).thenReturn(keycloak);
        when(keycloak.realm(REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(user.getId().toString())).thenReturn(userResource);
        doThrow(new RuntimeException("Keycloak error")).when(userResource).resetPassword(any());

        assertThrows(ExternalIdentityProviderException.class,
                () -> passwordChangeService.resetPassword(resetRequest));

        verify(tokenRepository).save(passwordResetToken);
        assertNotNull(passwordResetToken.getUsedAt());
        verify(tokenRepository, times(2)).findByToken(resetTokenValue);
    }
}
