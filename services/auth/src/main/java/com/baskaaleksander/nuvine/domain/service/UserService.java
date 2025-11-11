package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.RegisterRequest;
import com.baskaaleksander.nuvine.infrastrucure.config.KeycloakClientProvider;
import com.baskaaleksander.nuvine.infrastrucure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final KeycloakClientProvider keycloakClientProvider;
    private final UserRepository repository;

    @Transactional
    public UUID register(RegisterRequest request) {
        return null;
    }
}
