package com.dmc.archiving.billing.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the Stripe-key prod-gating policy (mirrors AuthSecurityConfigTest): an
 * explicit key is always honoured; an unset key falls back to the placeholder only
 * on local/dev/test (or no profile) and fails closed under any deployment profile.
 */
class StripeConfigTest {

    @Test
    void honoursAnExplicitKeyEvenInProd() {
        assertThat(StripeConfig.resolveApiKey(new String[]{"prod"}, "sk_live_real")).isEqualTo("sk_live_real");
    }

    @Test
    void prodWithNoKeyFailsClosed() {
        assertThatThrownBy(() -> StripeConfig.resolveApiKey(new String[]{"prod"}, ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("STRIPE_API_KEY");
    }

    @Test
    void dockerProfileWithNoKeyFailsClosed() {
        assertThatThrownBy(() -> StripeConfig.resolveApiKey(new String[]{"docker"}, ""))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void anyNonLocalProfileAlongsideLocalStillFailsClosed() {
        assertThatThrownBy(() -> StripeConfig.resolveApiKey(new String[]{"local", "staging"}, ""))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void localWithNoKeyFallsBackToPlaceholder() {
        assertThat(StripeConfig.resolveApiKey(new String[]{"local"}, ""))
                .isEqualTo(StripeConfig.DEV_FALLBACK_KEY);
    }

    @Test
    void noActiveProfileWithNoKeyFallsBackToPlaceholder() {
        assertThat(StripeConfig.resolveApiKey(new String[]{}, "  "))
                .isEqualTo(StripeConfig.DEV_FALLBACK_KEY);
    }
}
