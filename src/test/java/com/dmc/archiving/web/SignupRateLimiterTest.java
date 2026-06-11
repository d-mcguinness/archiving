package com.dmc.archiving.web;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the per-key fixed-window limiter: allows up to N per window then
 * blocks, refills when the window rolls over, and keeps keys independent. Uses a
 * controllable clock so refill is tested without sleeping.
 */
class SignupRateLimiterTest {

    @Test
    void allowsUpToTheLimitThenBlocksWithinTheWindow() {
        SignupRateLimiter limiter = new SignupRateLimiter(3, 60_000L, () -> 0L);

        assertThat(limiter.tryAcquire("ip")).isTrue();
        assertThat(limiter.tryAcquire("ip")).isTrue();
        assertThat(limiter.tryAcquire("ip")).isTrue();
        assertThat(limiter.tryAcquire("ip")).isFalse(); // 4th in the same window
        assertThat(limiter.tryAcquire("ip")).isFalse();
    }

    @Test
    void refillsAfterTheWindowElapses() {
        AtomicLong now = new AtomicLong(0L);
        SignupRateLimiter limiter = new SignupRateLimiter(2, 60_000L, now::get);

        assertThat(limiter.tryAcquire("ip")).isTrue();
        assertThat(limiter.tryAcquire("ip")).isTrue();
        assertThat(limiter.tryAcquire("ip")).isFalse();

        now.set(60_000L); // window boundary reached -> fresh window
        assertThat(limiter.tryAcquire("ip")).isTrue();
        assertThat(limiter.tryAcquire("ip")).isTrue();
        assertThat(limiter.tryAcquire("ip")).isFalse();
    }

    @Test
    void keysAreLimitedIndependently() {
        SignupRateLimiter limiter = new SignupRateLimiter(1, 60_000L, () -> 0L);

        assertThat(limiter.tryAcquire("ip-a")).isTrue();
        assertThat(limiter.tryAcquire("ip-a")).isFalse(); // a exhausted
        assertThat(limiter.tryAcquire("ip-b")).isTrue();   // b unaffected
    }
}
