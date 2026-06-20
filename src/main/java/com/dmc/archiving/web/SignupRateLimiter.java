package com.dmc.archiving.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.function.LongSupplier;

/**
 * Per-IP {@link FixedWindowRateLimiter} for the public signup endpoint, to blunt
 * automated account/tenant creation and credential probing. Every attempt
 * (success or failure) consumes one slot in the window.
 *
 * <p>Configured via {@code app.signup.rate-limit.max-attempts} /
 * {@code .window-minutes}.
 */
@Component
public class SignupRateLimiter extends FixedWindowRateLimiter {

    @Autowired
    public SignupRateLimiter(
            @Value("${app.signup.rate-limit.max-attempts:5}") int maxAttempts,
            @Value("${app.signup.rate-limit.window-minutes:15}") long windowMinutes) {
        super(maxAttempts, windowMinutes * 60_000L, System::currentTimeMillis);
    }

    /** Test/explicit constructor. */
    SignupRateLimiter(int maxAttempts, long windowMillis, LongSupplier clock) {
        super(maxAttempts, windowMillis, clock);
    }
}
