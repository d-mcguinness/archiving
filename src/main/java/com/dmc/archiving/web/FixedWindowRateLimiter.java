package com.dmc.archiving.web;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Per-key (typically client IP) fixed-window rate limiter. Every {@link #tryAcquire}
 * consumes one slot in the current window; once {@code maxAttempts} is reached the
 * key is blocked until the window rolls over. In-memory and single-instance —
 * adequate for this demo; a distributed deployment would back it with Redis, and a
 * shared egress IP (NAT) shares one window.
 *
 * <p>Thread-safe via atomic per-key updates; the key map is size-bounded by
 * sweeping expired windows so distinct keys can't grow it without limit. Subclasses
 * supply the concrete limits/clock (and Spring wiring) per protected endpoint.
 */
public abstract class FixedWindowRateLimiter {

    /** Above this many tracked keys, sweep expired windows to bound memory. */
    private static final int SWEEP_THRESHOLD = 10_000;

    private final int maxAttempts;
    private final long windowMillis;
    private final LongSupplier clock;
    // value = [windowStartMillis, countInWindow]
    private final Map<String, long[]> windows = new ConcurrentHashMap<>();

    protected FixedWindowRateLimiter(int maxAttempts, long windowMillis, LongSupplier clock) {
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
