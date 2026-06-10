package com.dmc.archiving.auth;

import com.dmc.archiving.auth.api.AuthContext;
import com.dmc.archiving.auth.api.TokenSigner;
import org.junit.jupiter.api.Test;

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
