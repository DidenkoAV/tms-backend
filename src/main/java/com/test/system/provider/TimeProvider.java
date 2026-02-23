package com.test.system.provider;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/**
 * Provider for current time.
 * Allows for testable time-dependent code by using Clock abstraction.
 */
@Component
public class TimeProvider {

    private final Clock clock;

    /**
     * Create time provider with system default clock.
     */
    public TimeProvider() {
        this(Clock.systemDefaultZone());
    }

    /**
     * Create time provider with custom clock (for testing).
     */
    public TimeProvider(Clock clock) {
        this.clock = clock;
    }

    /**
     * Get current instant.
     *
     * @return current instant from the clock
     */
    public Instant now() {
        return Instant.now(clock);
    }

    /**
     * Get the underlying clock.
     *
     * @return the clock
     */
    public Clock getClock() {
        return clock;
    }
}

