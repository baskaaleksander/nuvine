package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.PasswordChangeRequest;
import com.baskaaleksander.nuvine.domain.exception.ExternalIdentityProviderException;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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

    @BeforeEach
    void setUp() {
        email = "user@example.com";
        request = new PasswordChangeRequest("old-password", "NewPassword#1", "NewPassword#1");
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
}
