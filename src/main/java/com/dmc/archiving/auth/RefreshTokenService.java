package com.dmc.archiving.auth;

import com.dmc.archiving.auth.model.RefreshToken;
import com.dmc.archiving.auth.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;
import java.util.function.LongSupplier;

/**
 * Issues, rotates, and revokes opaque refresh tokens (the longer-lived credential
 * behind the short access token). State lives in {@code refresh_tokens}; only the
 * SHA-256 hash of each token is stored, never the plaintext.
 *
 * <p>Refresh is SINGLE-USE with ROTATION: {@link #rotate} consumes the presented
 * token (marks it revoked, links it to a freshly-minted successor it returns) so
 * the caller can re-issue a current access token AND hand back the next refresh
 * token. Re-presenting an already-rotated token is REUSE — a stolen-token signal —
 * and revokes the user's whole token set ({@link RefreshTokenRepository#revokeAllByUserId}).
 *
 * <p>TTL + clock are injectable (mirrors {@code TokenSigner}) so expiry/rotation
 * are deterministically unit-testable with a controllable clock.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    /** 256 bits of entropy, hex-encoded — opaque and unguessable. */
    private static final int TOKEN_BYTES = 32;

    /** Outcome of a successful rotation: the owner's id and the successor plaintext. */
    public record Rotation(Long userId, String refreshToken) {}

    private final RefreshTokenRepository repository;
    private final long ttlMillis;
    private final LongSupplier clock;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public RefreshTokenService(RefreshTokenRepository repository,
                               @Value("${app.auth.refresh-token-ttl-days:14}") long ttlDays) {
        this(repository, ttlDays * 24 * 60 * 60_000L, System::currentTimeMillis);
    }

    public RefreshTokenService(RefreshTokenRepository repository, long ttlMillis, LongSupplier clock) {
        this.repository = repository;
        this.ttlMillis = ttlMillis;
        this.clock = clock;
    }

    /**
     * Mint a fresh refresh token for a user, persist its hash, and return the
     * plaintext (the only time it is ever visible). Called at login and register.
     */
    @Transactional
    public String generate(Long userId) {
        return persistNew(userId).plaintext();
    }

    /**
     * Consume a refresh token and rotate it: revoke the presented row, mint a
     * linked successor, and return the owner's id together with the successor
     * plaintext. Empty for an unknown, expired, logged-out, or reused token.
     * Reuse of an already-rotated token revokes the user's whole token set.
     */
    @Transactional
    public Optional<Rotation> rotate(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return Optional.empty();
        }
        Optional<RefreshToken> found = repository.findByTokenHash(hash(plaintext));
        if (found.isEmpty()) {
            return Optional.empty();
        }
        RefreshToken token = found.get();

        if (token.isRevoked()) {
            // Already rotated (has a successor) → REUSE of a consumed token, the
            // stolen-refresh-token signal: kill every live token for this user.
            // Revoked without a successor → an explicit logout; just reject it.
            if (token.getRotatedToId() != null) {
                log.warn("Refresh-token reuse detected for user {} — revoking all sessions", token.getUserId());
                repository.revokeAllByUserId(token.getUserId());
            }
            return Optional.empty();
        }
        if (clock.getAsLong() >= token.getExpiresAt()) {
            return Optional.empty(); // expired → client must re-authenticate
        }

        // Valid: revoke this one and link it to a freshly-minted successor.
        Issued successor = persistNew(token.getUserId());
        token.setRevoked(true);
        token.setRotatedToId(successor.row().getId());
        repository.save(token);

        return Optional.of(new Rotation(token.getUserId(), successor.plaintext()));
    }

    /**
     * Revoke a specific refresh token (logout, device-scoped). No-op if it is
     * unknown or already revoked. Does NOT trip reuse detection.
     */
    @Transactional
    public void revoke(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return;
        }
        repository.findByTokenHash(hash(plaintext)).ifPresent(token -> {
            if (!token.isRevoked()) {
                token.setRevoked(true);
                repository.save(token);
            }
        });
    }

    /** Revoke every live token for a user ("sign out everywhere" / account-level kill). */
    @Transactional
    public int revokeAllForUser(Long userId) {
        return repository.revokeAllByUserId(userId);
    }

    /** A freshly persisted token row together with its (only-once-visible) plaintext. */
    private record Issued(RefreshToken row, String plaintext) {}

    private Issued persistNew(Long userId) {
        String plaintext = randomToken();
        RefreshToken row = new RefreshToken();
        row.setTokenHash(hash(plaintext));
        row.setUserId(userId);
        row.setExpiresAt(clock.getAsLong() + ttlMillis);
        row.setRevoked(false);
        row.setCreatedAt(now());
        return new Issued(repository.save(row), plaintext);
    }

    private String randomToken() {
        byte[] buf = new byte[TOKEN_BYTES];
        random.nextBytes(buf);
        return HexFormat.of().formatHex(buf);
    }

    private static String hash(String plaintext) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha.digest(plaintext.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(clock.getAsLong()), ZoneOffset.UTC);
    }
}
