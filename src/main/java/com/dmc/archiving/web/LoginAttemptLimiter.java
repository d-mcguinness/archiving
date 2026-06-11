package com.dmc.archiving.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Per-key failed-login limiter to blunt brute-force / password-spraying. Unlike
 * the signup limiter, this only counts FAILURES, is checked BEFORE the password is
 * verified, and is cleared on success — so legitimate users are never locked out
 * by their own repeated successful logins. The caller throttles on two keys: the
 * username (focused guessing of one account) and the client IP (spraying many
 * accounts from one source); a block on either stops the attempt.
 *
 * <p>Lockout is a temporary fixed window (not permanent), bounding the
 * account-lockout-DoS risk of username keying. In-memory + single-instance
 * (a distributed deploy should back it with Redis); thread-safe; size-bounded.
 * Configured via {@code app.login.rate-limit.max-failures} / {@code .window-minutes}.
 */
@Component
public class LoginAttemptLimiter {

    private static final int SWEEP_THRESHOLD = 10_000;

    private final int maxFailures;
    private final long windowMillis;
    private final LongSupplier clock;
    // value = [windowStartMillis, failureCount]
    private final Map<String, long[]> failures = new ConcurrentHashMap<>();

    @Autowired
    public LoginAttemptLimiter(
            @Value("${app.login.rate-limit.max-failures:10}") int maxFailures,
            @Value("${app.login.rate-limit.window-minutes:15}") long windowMinutes) {
        this(maxFailures, windowMinutes * 60_000L, System::currentTimeMillis);
    }

    /** Test/explicit constructor. */
    LoginAttemptLimiter(int maxFailures, long windowMillis, LongSupplier clock) {
        this.maxFailures = maxFailures;
        this.windowMillis = windowMillis;
        this.clock = clock;
    }

    /** True if this key has reached the failure limit within the current window. */
    public boolean isBlocked(String key) {
        long[] w = failures.get(key);
        return w != null && clock.getAsLong() - w[0] < windowMillis && w[1] >= maxFailures;
    }

    /** Record one failed login for this key (starting or extending the window). */
    public void recordFailure(String key) {
        long now = clock.getAsLong();
        failures.compute(key, (k, w) -> {
            if (w == null || now - w[0] >= windowMillis) {
                return new long[]{now, 1}; // fresh window
            }
            w[1]++;
            return w;
        });
        if (failures.size() > SWEEP_THRESHOLD) {
            failures.values().removeIf(w -> now - w[0] >= windowMillis);
        }
    }

    /** Clear the counter for this key after a successful login. */
    public void recordSuccess(String key) {
        failures.remove(key);
    }
}
