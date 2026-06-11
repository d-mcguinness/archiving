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
 * server-held secret. A token therefore cannot be forged, nor its
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

            // Split with -1 (keep trailing empties) and require EXACTLY 5 segments.
            // userId/expiresAt are numeric, role is allow-listed, the nonce is a UUID —
            // none contain '_'. A '_' in the username (the only attacker-influenced
            // field) would yield != 5 segments and is rejected here, so a crafted
            // username cannot shift the parse to read role=ADMIN. Usernames are also
            // charset-restricted at registration; this is defence-in-depth.
            String[] parts = payload.split("_", -1); // userId_username_role_expiresAt_nonce
            if (parts.length != 5) {
                return AuthContext.ANONYMOUS;
            }
            Long userId = Long.valueOf(parts[0]);     // tamper-proof: covered by the HMAC
            String username = parts[1];
            String role = parts[2];
            long expiresAt = Long.parseLong(parts[3]); // signed, so cannot be extended by the client
            if (username.isEmpty() || !isAllowedRole(role)) {
                return AuthContext.ANONYMOUS;
            }
            if (clock.getAsLong() >= expiresAt) {
                return AuthContext.ANONYMOUS; // expired
            }
            return new AuthContext(userId, role, username);
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
