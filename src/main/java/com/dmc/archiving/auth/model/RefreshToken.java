package com.dmc.archiving.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A persisted, single-use refresh token. Only the SHA-256 HASH of the opaque
 * token is stored ({@code token_hash}) — never the plaintext — so a database
 * read cannot mint a usable token.
 *
 * <p>Rotation: each successful refresh marks the presented row {@code revoked}
 * and records {@code rotatedToId} (the id of the freshly-issued successor),
 * forming a per-session chain. Re-presenting a row that is already
 * {@code revoked && rotatedToId != null} is REUSE of a rotated token — the
 * classic stolen-refresh-token signal — and revokes the whole user's tokens.
 * A row revoked WITHOUT a successor (id-less) was killed by an explicit logout
 * and is simply rejected, not treated as theft.
 *
 * <p>This entity is internal to the auth module (the package is unexposed), so
 * no other module can reach it; the per-request auth hot path stays stateless.
 */
@Entity
@Table(name = "refresh_tokens", indexes = @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** SHA-256 hex of the opaque token; the plaintext is returned to the client only once. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Expiry as epoch millis, compared against the injectable clock (matches TokenSigner's model). */
    @Column(name = "expires_at", nullable = false)
    private long expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    /** Id of the successor row this one rotated into; non-null only once rotated. */
    @Column(name = "rotated_to_id")
    private Long rotatedToId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
