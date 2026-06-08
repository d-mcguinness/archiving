package com.dmc.archiving.auth;

import com.dmc.archiving.auth.api.TokenSigner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

/**
 * Builds the {@link TokenSigner} bean and enforces the prod-gating policy for
 * the demo auth scheme. The HMAC secret comes from {@code app.auth.token-secret}
 * (env {@code APP_AUTH_TOKEN_SECRET}). When it is unset:
 * <ul>
 *   <li>a non-prod profile falls back to a built-in DEV secret (logged loudly —
 *       tokens are demo-only and forgeable by anyone with repo access), so the
 *       app still boots for local development;</li>
 *   <li>the {@code prod} profile fails closed and refuses to start, so
 *       production can never run with a publicly known signing secret.</li>
 * </ul>
 */
@Configuration
public class AuthSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(AuthSecurityConfig.class);

    /** Public, repo-visible secret — acceptable ONLY for local/demo use. */
    static final String DEV_FALLBACK_SECRET = "dev-insecure-demo-secret-do-not-use-in-prod";
    static final String PROD_PROFILE = "prod";

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
        if (Arrays.asList(activeProfiles).contains(PROD_PROFILE)) {
            throw new IllegalStateException(
                    "app.auth.token-secret (env APP_AUTH_TOKEN_SECRET) must be set under the "
                    + "'prod' profile — refusing to start with an insecure default auth secret.");
        }
        log.warn("No app.auth.token-secret set; using the built-in DEV auth secret. "
                + "Tokens are DEMO-ONLY and forgeable by anyone with repo access. "
                + "Set APP_AUTH_TOKEN_SECRET before any non-local deployment.");
        return new TokenSigner(DEV_FALLBACK_SECRET);
    }
}
