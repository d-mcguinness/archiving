package com.dmc.archiving.billing.config;

import com.stripe.StripeClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.Set;

/**
 * Builds the {@link StripeClient} bean and enforces prod-gating of the Stripe
 * secret key, mirroring {@code AuthSecurityConfig}. The key comes from
 * {@code app.billing.stripe.api-key} (env {@code STRIPE_API_KEY}). When unset:
 * <ul>
 *   <li>a clearly-local run (no active profile, or only {@code local}/{@code dev}/
 *       {@code test}) falls back to a placeholder key (logged loudly — billing is
 *       effectively DISABLED; any Stripe call would fail), so the app and tests
 *       still boot without a real key;</li>
 *   <li>ANY other (deployment-grade) profile — {@code docker}, {@code staging},
 *       {@code prod}, … — fails closed and refuses to start, so a real deployment
 *       can never run with a placeholder billing key.</li>
 * </ul>
 * No Stripe network call is made at construction; the client is configured only.
 * PCI scope stays at SAQ-A — card data never touches this server (Checkout/Portal).
 */
@Configuration
public class StripeConfig {

    private static final Logger log = LoggerFactory.getLogger(StripeConfig.class);

    /** Obvious non-functional placeholder — acceptable ONLY for local/demo where billing is off. */
    static final String DEV_FALLBACK_KEY = "sk_test_PLACEHOLDER_billing_disabled_set_STRIPE_API_KEY";
    /** Profiles under which the placeholder is acceptable; everything else must supply a real key. */
    static final Set<String> LOCAL_PROFILES = Set.of("local", "dev", "test");

    @Bean
    StripeClient stripeClient(Environment environment,
                              @Value("${app.billing.stripe.api-key:}") String configuredKey) {
        return new StripeClient(resolveApiKey(environment.getActiveProfiles(), configuredKey));
    }

    /** Package-visible so the gating policy can be unit-tested without a context. */
    static String resolveApiKey(String[] activeProfiles, String configuredKey) {
        if (configuredKey != null && !configuredKey.isBlank()) {
            return configuredKey;
        }
        boolean localOnly = Arrays.stream(activeProfiles).allMatch(LOCAL_PROFILES::contains);
        if (!localOnly) {
            throw new IllegalStateException(
                    "app.billing.stripe.api-key (env STRIPE_API_KEY) must be set under profile(s) "
                    + Arrays.toString(activeProfiles) + " — refusing to start billing with a placeholder key. "
                    + "The placeholder is allowed only with no profile or local/dev/test.");
        }
        log.warn("No app.billing.stripe.api-key set; using a PLACEHOLDER key (profiles={}). Billing is DISABLED — "
                + "any Stripe call will fail. Set STRIPE_API_KEY before any non-local deployment.",
                Arrays.toString(activeProfiles));
        return DEV_FALLBACK_KEY;
    }
}
