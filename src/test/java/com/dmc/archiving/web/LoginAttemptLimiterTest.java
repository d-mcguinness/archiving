package com.dmc.archiving.web;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the failed-login limiter: blocks only after N failures within the
 * window, a success clears the counter, the window rolls over (temporary
 * lockout), and keys are independent. Controllable clock — no sleeping.
 */
class LoginAttemptLimiterTest {

    @Test
    void blocksOnlyAfterReachingTheFailureLimit() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(3, 60_000L, () -> 0L);

        assertThat(limiter.isBlocked("k")).isFalse();
        limiter.recordFailure("k");
        limiter.recordFailure("k");
        assertThat(limiter.isBlocked("k")).isFalse(); // 2 < 3
        limiter.recordFailure("k");
        assertThat(limiter.isBlocked("k")).isTrue();   // 3 >= 3
    }

    @Test
    void successClearsTheCounter() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(2, 60_000L, () -> 0L);
        limiter.recordFailure("k");
        limiter.recordFailure("k");
        assertThat(limiter.isBlocked("k")).isTrue();

        limiter.recordSuccess("k");

        assertThat(limiter.isBlocked("k")).isFalse();
    }

    @Test
    void lockoutIsTemporary_windowRollsOver() {
        AtomicLong now = new AtomicLong(0L);
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(2, 60_000L, now::get);
        limiter.recordFailure("k");
        limiter.recordFailure("k");
        assertThat(limiter.isBlocked("k")).isTrue();

        now.set(60_000L); // window elapsed
        assertThat(limiter.isBlocked("k")).isFalse();
    }

    @Test
    void keysAreIndependent() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(1, 60_000L, () -> 0L);
        limiter.recordFailure("u:victim");
        assertThat(limiter.isBlocked("u:victim")).isTrue();
        assertThat(limiter.isBlocked("ip:203.0.113.1")).isFalse();
    }
}
