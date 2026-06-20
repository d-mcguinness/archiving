package com.dmc.archiving.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.function.LongSupplier;

/**
 * Per-IP {@link FixedWindowRateLimiter} for the unauthenticated, DB-writing
 * refresh-token endpoints (POST /api/auth/refresh and /api/auth/logout). Caps the
 * DoS/amplification surface and blunts an attacker replaying a stale token to fire
 * the reuse-detection nuke repeatedly. The cap is generous because a legitimate
 * client (single-flight on the SPA) refreshes only around access-token expiry; it
 * is sized to stop scripted abuse, not to limit normal multi-tab/NAT traffic.
 *
 * <p>Configured via {@code app.refresh.rate-limit.max-attempts} /
 * {@code .window-minutes}.
 */
@Component
public class RefreshRateLimiter extends FixedWindowRateLimiter {

    @Autowired
    public RefreshRateLimiter(
            @Value("${app.refresh.rate-limit.max-attempts:60}") int maxAttempts,
            @Value("${app.refresh.rate-limit.window-minutes:15}") long windowMinutes) {
        super(maxAttempts, windowMinutes * 60_000L, System::currentTimeMillis);
    }

    /** Test/explicit constructor. */
    RefreshRateLimiter(int maxAttempts, long windowMillis, LongSupplier clock) {
        super(maxAttempts, windowMillis, clock);
    }
}
