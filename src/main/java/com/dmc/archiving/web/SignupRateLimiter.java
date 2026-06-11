package com.dmc.archiving.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Per-key (client IP) fixed-window rate limiter for the public signup endpoint, to
 * blunt automated account/tenant creation and credential probing. In-memory and
 * single-instance — adequate for this demo; a distributed deployment would back it
 * with Redis. Every attempt (success or failure) consumes one slot in the window.
 *
 * <p>Configured via {@code app.signup.rate-limit.max-attempts} /
 * {@code .window-minutes}. Thread-safe via atomic per-key updates; the key map is
 * size-bounded by sweeping expired windows so distinct IPs can't grow it without
 * limit.
 */
@Component
public class SignupRateLimiter {

    /** Above this many tracked keys, sweep expired windows to bound memory. */
    private static final int SWEEP_THRESHOLD = 10_000;

    private final int maxAttempts;
    private final long windowMillis;
    private final LongSupplier clock;
    // value = [windowStartMillis, countInWindow]
    private final Map<String, long[]> windows = new ConcurrentHashMap<>();

    @Autowired
    public SignupRateLimiter(
            @Value("${app.signup.rate-limit.max-attempts:5}") int maxAttempts,
            @Value("${app.signup.rate-limit.window-minutes:15}") long windowMinutes) {
        this(maxAttempts, windowMinutes * 60_000L, System::currentTimeMillis);
    }

    /** Test/explicit constructor. */
    SignupRateLimiter(int maxAttempts, long windowMillis, LongSupplier clock) {
        this.maxAttempts = maxAttempts;
        this.windowMillis = windowMillis;
        this.clock = clock;
    }

    /** True if this key may proceed (and consumes a slot); false if it is over the limit. */
    public boolean tryAcquire(String key) {
        long now = clock.getAsLong();
        boolean[] allowed = {false};
        windows.compute(key, (k, w) -> {
            if (w == null || now - w[0] >= windowMillis) {
                allowed[0] = true;
                return new long[]{now, 1}; // fresh window
            }
            if (w[1] < maxAttempts) {
                allowed[0] = true;
                w[1]++;
                return w;
            }
            allowed[0] = false; // within window, limit reached
            return w;
        });
        if (windows.size() > SWEEP_THRESHOLD) {
            windows.values().removeIf(w -> now - w[0] >= windowMillis);
        }
        return allowed[0];
    }
}
