package com.dmc.archiving.auth.api;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Issues and verifies the app's bearer token. The token is
 * {@code Bearer_{userId}_{username}_{role}_{expiresAt}_{nonce}_{hmac}}, where the
 * HMAC-SHA256 covers {@code userId_username_role_expiresAt_nonce} under a
 * server-held secret. An impersonation token (issued to an admin or tenant
 * owner acting as another user) appends a sixth signed segment
 * {@code act<actorId>}; see {@link #issueImpersonation}. A token therefore cannot be forged, nor its
 * userId/role/username/expiry tampered, without that secret. The userId is the
 * REAL persisted user id, carried in the signed payload (no username-to-id
 * mapping), so it is correct for DB-backed accounts.
 *
 * <p>Tokens EXPIRE: {@code expiresAt} (epoch millis, signed) is set to
 * now + TTL at issue, and {@link #verify} rejects a token once the clock reaches
 * it — so a leaked token is valid only until expiry, not forever.
 *
 * <p>Deterministic and Spring-free for direct unit testing (the TTL and clock are
 * injectable); the secret-resolution / prod-gating policy lives in
 * {@link com.dmc.archiving.auth.AuthSecurityConfig}.
 */
public final class TokenSigner {

    private static final String PREFIX = "Bearer_";
    private static final String HMAC_ALG = "HmacSHA256";
    /** Default TTL when only a secret is supplied (back-compat / tests): 1 hour. */
    private static final long DEFAULT_TTL_MILLIS = 60 * 60_000L;
    /** Impersonation tokens never outlive this, whatever the normal TTL: 30 minutes. */
    static final long IMPERSONATION_MAX_TTL_MILLIS = 30 * 60_000L;
    private static final String ACTOR_PREFIX = "act";

    private final byte[] secret;
    private final long ttlMillis;
    private final LongSupplier clock;

    public TokenSigner(String secret) {
        this(secret, DEFAULT_TTL_MILLIS, System::currentTimeMillis);
    }

    public TokenSigner(String secret, long ttlMillis, LongSupplier clock) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("Token secret must not be blank");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.ttlMillis = ttlMillis;
        this.clock = clock;
    }

    /** TTL of issued tokens in seconds (for the login/register response's expiresIn). */
    public long ttlSeconds() {
        return ttlMillis / 1000;
    }

    /** Issue a freshly signed token carrying the real id, username, role, and expiry. */
    public String issue(Long userId, String username, String role) {
        long expiresAt = clock.getAsLong() + ttlMillis;
        String payload = userId + "_" + username + "_" + role + "_" + expiresAt + "_" + UUID.randomUUID();
        return PREFIX + payload + "_" + sign(payload);
    }

    /**
     * Issue a token for {@code userId} that records {@code actorId} as the person
     * really holding it. It verifies to an {@link AuthContext} for the target user
     * (so authorization behaves exactly as if they were signed in) with
     * {@link AuthContext#actorId()} set, expires after at most
     * {@link #IMPERSONATION_MAX_TTL_MILLIS}, and has no refresh token. Who may
     * obtain one is decided by the caller (AuthController).
     */
    public String issueImpersonation(Long userId, String username, String role, Long actorId) {
        if (actorId == null) {
            throw new IllegalArgumentException("actorId is required for an impersonation token");
        }
        long expiresAt = clock.getAsLong() + Math.min(ttlMillis, IMPERSONATION_MAX_TTL_MILLIS);
        String payload = userId + "_" + username + "_" + role + "_" + expiresAt + "_" + UUID.randomUUID()
                + "_" + ACTOR_PREFIX + actorId;
        return PREFIX + payload + "_" + sign(payload);
    }

    /** Seconds an impersonation token stays valid. */
    public long impersonationTtlSeconds() {
        return Math.min(ttlMillis, IMPERSONATION_MAX_TTL_MILLIS) / 1000;
    }

    /**
     * Verify the Authorization header and return its {@link AuthContext}.
     * Returns {@link AuthContext#ANONYMOUS} for any missing, malformed, tampered,
     * or EXPIRED token; verification never throws.
     */
    public AuthContext verify(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return AuthContext.ANONYMOUS;
        }
        try {
            // Tolerate an optional "Bearer " HTTP scheme prefix in front of the token.
            String token = authorizationHeader.startsWith("Bearer ")
                    ? authorizationHeader.substring(7)
                    : authorizationHeader;
            if (!token.startsWith(PREFIX)) {
                return AuthContext.ANONYMOUS;
            }

            String body = token.substring(PREFIX.length()); // payload + "_" + signature
            int lastSep = body.lastIndexOf('_');
            if (lastSep <= 0) {
                return AuthContext.ANONYMOUS;
            }
            String payload = body.substring(0, lastSep);
            String presentedSig = body.substring(lastSep + 1);
            if (!constantTimeEquals(sign(payload), presentedSig)) {
                return AuthContext.ANONYMOUS;
            }

            // Split with -1 (keep trailing empties) and require EXACTLY 5 segments —
            // or 6 for an impersonation token, whose extra segment must match
            // "act<digits>". userId/expiresAt are numeric, role is allow-listed, the
            // nonce is a UUID — none contain '_'. A '_' in the username (the only
            // attacker-influenced field) would shift the segment count/shape and is
            // rejected here, so a crafted username cannot read as role=ADMIN or as
            // an actor claim. Usernames are also charset-restricted at registration;
            // this is defence-in-depth.
            String[] parts = payload.split("_", -1); // userId_username_role_expiresAt_nonce[_act<actorId>]
            if (parts.length != 5 && parts.length != 6) {
                return AuthContext.ANONYMOUS;
            }
            Long userId = Long.valueOf(parts[0]);     // tamper-proof: covered by the HMAC
            String username = parts[1];
            String role = parts[2];
            long expiresAt = Long.parseLong(parts[3]); // signed, so cannot be extended by the client
            if (username.isEmpty() || !isAllowedRole(role)) {
                return AuthContext.ANONYMOUS;
            }
            Long actorId = null;
            if (parts.length == 6) {
                if (!parts[5].startsWith(ACTOR_PREFIX) || parts[5].length() <= ACTOR_PREFIX.length()) {
                    return AuthContext.ANONYMOUS;
                }
                actorId = Long.valueOf(parts[5].substring(ACTOR_PREFIX.length())); // NumberFormatException → ANONYMOUS
            }
            if (clock.getAsLong() >= expiresAt) {
                return AuthContext.ANONYMOUS; // expired
            }
            return new AuthContext(userId, role, username, actorId);
        } catch (Exception e) {
            // Includes NumberFormatException on a non-numeric userId/expiresAt segment.
            return AuthContext.ANONYMOUS;
        }
    }

    private static boolean isAllowedRole(String role) {
        return "ADMIN".equals(role) || "TENANT".equals(role) || "USER".equals(role);
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALG);
            mac.init(new SecretKeySpec(secret, HMAC_ALG));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC signing failed", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
