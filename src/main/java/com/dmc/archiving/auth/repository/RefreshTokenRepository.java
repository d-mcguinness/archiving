package com.dmc.archiving.auth.repository;

import com.dmc.archiving.auth.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Atomically claim a token for rotation: flip revoked false→true and link its
     * successor, but ONLY if it is still live. Returns the affected row count, so
     * exactly one of N concurrent refreshes of the same token wins (1); the losers
     * see 0 and must reject. This single conditional UPDATE is what makes
     * single-use rotation race-safe under READ_COMMITTED without row locking.
     */
    @Modifying
    @Query("update RefreshToken r set r.revoked = true, r.rotatedToId = :successorId "
            + "where r.id = :id and r.revoked = false")
    int consumeForRotation(@Param("id") Long id, @Param("successorId") Long successorId);

    /** Revoke every still-live token for a user (reuse-detection nuke, "sign out everywhere"). */
    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.userId = :userId and r.revoked = false")
    int revokeAllByUserId(@Param("userId") Long userId);
}
