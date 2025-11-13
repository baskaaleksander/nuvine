package com.baskaaleksander.nuvine.infrastrucure.repository;

import com.baskaaleksander.nuvine.domain.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    @Modifying
    @Query("update RefreshToken t set t.revoked = true where t.user.email = :email and t.revoked = false")
    void revokeAllTokensByEmail(String email);

    @Modifying
    @Query("update RefreshToken t set t.revoked = true where t.token = :token")
    void revokeToken(String token);

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("update RefreshToken t set t.usedAt = :usedAt where t.id = :tokenId")
    void updateUsedAt(Instant usedAt, UUID tokenId);
}
