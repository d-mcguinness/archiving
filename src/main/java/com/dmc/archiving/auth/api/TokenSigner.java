package com.dmc.archiving.auth.api;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Issues and verifies the app's bearer token. The token is
 * {@code Bearer_{username}_{role}_{nonce}_{hmac}}, where the HMAC-SHA256 covers
 * {@code username_role_nonce} under a server-held secret. A token therefore
 * cannot be forged, nor its role/username tampered, without that secret —
 * replacing the old unsigned scheme that let anyone present
 * {@code Bearer_x_ADMIN_0} and assume any identity.
 *
 * <p>This class is deterministic and Spring-free so it can be unit-tested
 * directly; the secret-resolution and prod-gating policy lives in
 * {@link com.dmc.archiving.auth.AuthSecurityConfig}.
 */
public final class TokenSigner {

    private static final String PREFIX = "Bearer_";
    private static final String HMAC_ALG = "HmacSHA256";

    private final byte[] secret;

    public TokenSigner(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("Token secret must not be blank");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    /** Issue a freshly signed token for an authenticated user. */
    public String issue(String username, String role) {
        String payload = username + "_" + role + "_" + UUID.randomUUID();
        return PREFIX + payload + "_" + sign(payload);
    }

    /**
     * Verify the Authorization header and return its {@link AuthContext}.
     * Returns {@link AuthContext#ANONYMOUS} for any missing, malformed, or
     * tampered token; verification never throws.
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

            String[] parts = payload.split("_", 3); // username_role_nonce
            if (parts.length < 3) {
                return AuthContext.ANONYMOUS;
            }
            String username = parts[0];
            String role = parts[1];
            return new AuthContext(mapUserId(username), role, username);
        } catch (Exception e) {
            return AuthContext.ANONYMOUS;
        }
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

    private static Long mapUserId(String username) {
        return switch (username.toLowerCase()) {
            case "admin" -> 1L;
            case "tenant" -> 2L;
            case "user" -> 3L;
            default -> 999L;
        };
    }
}
