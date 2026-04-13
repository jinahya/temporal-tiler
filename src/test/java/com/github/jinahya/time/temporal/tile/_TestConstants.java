package com.github.jinahya.time.temporal.tile;

import java.time.temporal.ChronoField;

/**
 * Shared constants used across test classes.
 */
final class _TestConstants {

    static final long NUMBER_OF_DAYS_OF_A_WEEK = ChronoField.DAY_OF_WEEK.range().getMaximum();

    private _TestConstants() {
        throw new AssertionError("instantiation is not allowed");
    }
}
