package com.dmc.archiving.auth;

import com.dmc.archiving.auth.api.AuthContext;
import com.dmc.archiving.auth.api.TokenSigner;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the bearer token is unforgeable (Review M1): a token issued under the
 * server secret round-trips to its identity (the REAL userId is carried in the
 * signed payload), while any missing, tampered, or differently-signed token
 * resolves to ANONYMOUS rather than an assumed identity.
 */
class TokenSignerTest {

    private final TokenSigner signer = new TokenSigner("server-secret");

    @Test
    void issuedTokenRoundTripsToItsIdentity() {
        AuthContext ctx = signer.verify(signer.issue(42L, "tenant", "TENANT"));

        assertThat(ctx.isAuthenticated()).isTrue();
        assertThat(ctx.role()).isEqualTo("TENANT");
        assertThat(ctx.username()).isEqualTo("tenant");
        assertThat(ctx.userId()).isEqualTo(42L); // carried, not derived from username
        assertThat(ctx.isTenant()).isTrue();
    }

    @Test
    void carriesTheRealUserId() {
        AuthContext ctx = signer.verify(signer.issue(7L, "admin", "ADMIN"));

        assertThat(ctx.isAdmin()).isTrue();
        assertThat(ctx.userId()).isEqualTo(7L);
    }

    @Test
    void toleratesBearerSchemePrefix() {
        AuthContext ctx = signer.verify("Bearer " + signer.issue(1L, "admin", "ADMIN"));

        assertThat(ctx.isAdmin()).isTrue();
        assertThat(ctx.userId()).isEqualTo(1L);
    }

    @Test
    void rejectsForgedUnsignedToken() {
        // The old scheme — anyone could mint this and become ADMIN.
        assertThat(signer.verify("Bearer_attacker_ADMIN_0")).isEqualTo(AuthContext.ANONYMOUS);
    }

    @Test
    void usernameWithUnderscoreCannotShiftTheParseToAdmin() {
        // Review-PR16 CRITICAL: a '_' in the username would shift the positional
        // parse so role reads "ADMIN" on a legitimately-signed token. verify now
        // requires exactly 5 segments, so this is rejected outright.
        AuthContext ctx = signer.verify(signer.issue(42L, "x_ADMIN", "TENANT"));

        assertThat(ctx).isEqualTo(AuthContext.ANONYMOUS);
        assertThat(ctx.isAdmin()).isFalse();
    }

    @Test
    void expiredTokenIsRejected() {
        AtomicLong now = new AtomicLong(0L);
        TokenSigner s = new TokenSigner("server-secret", 60_000L, now::get);
        String token = s.issue(42L, "tenant", "TENANT"); // expires at now+60s

        assertThat(s.verify(token).isAuthenticated()).isTrue(); // still valid at t=0

        now.set(60_000L); // clock reaches expiry
        assertThat(s.verify(token)).isEqualTo(AuthContext.ANONYMOUS);

        now.set(120_000L); // and well past it
        assertThat(s.verify(token)).isEqualTo(AuthContext.ANONYMOUS);
    }

    @Test
    void tokenStaysValidUntilTheInstantBeforeExpiry() {
        AtomicLong now = new AtomicLong(0L);
        TokenSigner s = new TokenSigner("server-secret", 60_000L, now::get);
        String token = s.issue(1L, "admin", "ADMIN");

        now.set(59_999L); // one millisecond shy of expiry
        assertThat(s.verify(token).isAdmin()).isTrue();
    }

    @Test
    void clientCannotExtendExpiry() {
        // With a fixed clock at t=0 and a 60s TTL, expiresAt is deterministically
        // 60000. Pushing it further out invalidates the HMAC (which covers it), so
        // the token cannot be kept alive by editing its expiry.
        AtomicLong now = new AtomicLong(0L);
        TokenSigner s = new TokenSigner("server-secret", 60_000L, now::get);
        String token = s.issue(2L, "tenant", "TENANT");
        String extended = token.replace("_60000_", "_9999999999999_");

        now.set(120_000L); // past the real expiry
        assertThat(s.verify(extended)).isEqualTo(AuthContext.ANONYMOUS);
    }

    @Test
    void ttlSecondsReflectsTheConfiguredTtl() {
        assertThat(new TokenSigner("server-secret", 60_000L, () -> 0L).ttlSeconds()).isEqualTo(60L);
        assertThat(new TokenSigner("server-secret").ttlSeconds()).isEqualTo(3600L); // default 1h
    }

    @Test
    void rejectsAnUnknownRole() {
        // Role must be allow-listed even though it's HMAC-signed (defence in depth).
        assertThat(signer.verify(signer.issue(1L, "alice", "SUPERUSER")))
                .isEqualTo(AuthContext.ANONYMOUS);
    }

    @Test
    void rejectsRoleTampering() {
        String tenantToken = signer.issue(2L, "tenant", "TENANT");
        // Swap the role in a validly-signed token; the signature no longer covers it.
        String escalated = tenantToken.replace("_TENANT_", "_ADMIN_");

        assertThat(signer.verify(escalated)).isEqualTo(AuthContext.ANONYMOUS);
    }

    @Test
    void rejectsTokenSignedWithADifferentSecret() {
        String foreign = new TokenSigner("other-secret").issue(1L, "admin", "ADMIN");

        assertThat(signer.verify(foreign)).isEqualTo(AuthContext.ANONYMOUS);
    }

    @Test
    void treatsMissingMalformedTokensAsAnonymous() {
        assertThat(signer.verify(null)).isEqualTo(AuthContext.ANONYMOUS);
        assertThat(signer.verify("")).isEqualTo(AuthContext.ANONYMOUS);
        assertThat(signer.verify("   ")).isEqualTo(AuthContext.ANONYMOUS);
        assertThat(signer.verify("garbage")).isEqualTo(AuthContext.ANONYMOUS);
        assertThat(signer.verify("Bearer_only")).isEqualTo(AuthContext.ANONYMOUS);
    }

    @Test
    void rejectsABlankSecret() {
        assertThatThrownBy(() -> new TokenSigner(" "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TokenSigner(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
