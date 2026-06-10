package com.dmc.archiving.auth;

import com.dmc.archiving.auth.api.AuthContext;
import com.dmc.archiving.auth.api.TokenSigner;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the prod-gating policy for the demo auth secret (Review M1): an
 * explicit secret is always honoured; an unset secret falls back to the DEV
 * secret only on non-prod profiles, and fails closed (refuses to build) under
 * the 'prod' profile so production never signs with a public secret.
 */
class AuthSecurityConfigTest {

    @Test
    void honoursAnExplicitSecretEvenInProd() {
        TokenSigner signer = AuthSecurityConfig.buildTokenSigner(new String[]{"prod"}, "real-secret");

        AuthContext ctx = signer.verify(signer.issue("admin", "ADMIN"));
        assertThat(ctx.isAdmin()).isTrue();
    }

    @Test
    void prodWithNoSecretFailsClosed() {
        assertThatThrownBy(() -> AuthSecurityConfig.buildTokenSigner(new String[]{"prod"}, ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_AUTH_TOKEN_SECRET");
    }

    @Test
    void dockerProfileWithNoSecretFailsClosed() {
        // The shipped compose profile is NOT local/dev/test, so the DEV fallback
        // must not apply — it has to supply a real secret (Review H1).
        assertThatThrownBy(() -> AuthSecurityConfig.buildTokenSigner(new String[]{"docker"}, ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_AUTH_TOKEN_SECRET");
    }

    @Test
    void anyNonLocalProfileWithLocalAlongsideStillFailsClosed() {
        // A non-local profile present alongside a local one is still deployment-grade.
        assertThatThrownBy(() -> AuthSecurityConfig.buildTokenSigner(new String[]{"local", "staging"}, ""))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testProfileWithNoSecretFallsBackToDevSecret() {
        TokenSigner signer = AuthSecurityConfig.buildTokenSigner(new String[]{"test"}, "");
        assertThat(signer.verify(signer.issue("admin", "ADMIN")).isAdmin()).isTrue();
    }

    @Test
    void nonProdWithNoSecretFallsBackToDevSecret() {
        TokenSigner signer = AuthSecurityConfig.buildTokenSigner(new String[]{"local"}, "");

        AuthContext ctx = signer.verify(signer.issue("user", "USER"));
        assertThat(ctx.isUser()).isTrue();
    }

    @Test
    void noActiveProfileWithNoSecretFallsBackToDevSecret() {
        TokenSigner signer = AuthSecurityConfig.buildTokenSigner(new String[]{}, "  ");

        AuthContext ctx = signer.verify(signer.issue("tenant", "TENANT"));
        assertThat(ctx.isTenant()).isTrue();
    }
}
