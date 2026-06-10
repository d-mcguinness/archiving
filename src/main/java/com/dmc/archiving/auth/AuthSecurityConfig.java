package com.dmc.archiving.auth;

import com.dmc.archiving.auth.api.TokenSigner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.Set;

/**
 * Builds the {@link TokenSigner} bean and enforces the prod-gating policy for
 * the demo auth scheme. The HMAC secret comes from {@code app.auth.token-secret}
 * (env {@code APP_AUTH_TOKEN_SECRET}). When it is unset:
 * <ul>
 *   <li>a clearly-local run (no active profile, or only {@code local}/{@code dev}/
 *       {@code test}) falls back to a built-in DEV secret (logged loudly — tokens
 *       are demo-only and forgeable by anyone with repo access), so the app and
 *       the test suite still boot;</li>
 *   <li>ANY other (named, deployment-grade) profile — {@code docker},
 *       {@code staging}, {@code prod}, … — fails closed and refuses to start, so
 *       a real deployment can never run with the publicly known signing secret.</li>
 * </ul>
 * Gating on a local-profile allowlist (rather than only the literal {@code prod})
 * is deliberate: the shipped {@code docker} compose profile must not silently use
 * the DEV secret.
 */
@Configuration
public class AuthSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(AuthSecurityConfig.class);

    /** Public, repo-visible secret — acceptable ONLY for local/demo use. */
    static final String DEV_FALLBACK_SECRET = "dev-insecure-demo-secret-do-not-use-in-prod";
    /** Profiles under which the DEV fallback is acceptable; everything else must supply a real secret. */
    static final Set<String> LOCAL_PROFILES = Set.of("local", "dev", "test");

    @Bean
    TokenSigner tokenSigner(Environment environment,
                            @Value("${app.auth.token-secret:}") String configuredSecret) {
        return buildTokenSigner(environment.getActiveProfiles(), configuredSecret);
    }

    /** Package-visible so the gating policy can be unit-tested without a context. */
    static TokenSigner buildTokenSigner(String[] activeProfiles, String configuredSecret) {
        if (configuredSecret != null && !configuredSecret.isBlank()) {
            return new TokenSigner(configuredSecret);
        }
        // Fail closed unless this is clearly a local/test run. A real secret is
        // required for every named deployment profile (docker, staging, prod, …),
        // not just "prod". Plain local runs and the test suite use no profile.
        boolean localOnly = Arrays.stream(activeProfiles).allMatch(LOCAL_PROFILES::contains);
        if (!localOnly) {
            throw new IllegalStateException(
                    "app.auth.token-secret (env APP_AUTH_TOKEN_SECRET) must be set under profile(s) "
                    + Arrays.toString(activeProfiles) + " — refusing to start with the built-in DEV auth "
                    + "secret. The DEV fallback is allowed only with no profile or local/dev/test.");
        }
        log.warn("No app.auth.token-secret set; using the built-in DEV auth secret (profiles={}). "
                + "Tokens are DEMO-ONLY and forgeable by anyone with repo access. "
                + "Set APP_AUTH_TOKEN_SECRET before any non-local deployment.", Arrays.toString(activeProfiles));
        return new TokenSigner(DEV_FALLBACK_SECRET);
    }
}
