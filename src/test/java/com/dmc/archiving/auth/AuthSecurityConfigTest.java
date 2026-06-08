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
