package com.baskaaleksander.nuvine.infrastrucure.repository;

import com.baskaaleksander.nuvine.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmail(String email);
}
