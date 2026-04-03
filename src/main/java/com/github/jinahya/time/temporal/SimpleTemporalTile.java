package com.github.jinahya.time.temporal;

import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.util.Objects;

/**
 * A simple tile representing a half-open temporal range {@code [startInclusive, endExclusive)} produced by a single
 * {@link TemporalUnit}.
 *
 * @param <T> the temporal type
 */
public final class SimpleTemporalTile<T extends Temporal & Comparable<? super T>> {

    SimpleTemporalTile(final T startInclusive, final T endExclusive, final TemporalUnit unit) {
        this.startInclusive = Objects.requireNonNull(startInclusive, "startInclusive is null");
        this.endExclusive = Objects.requireNonNull(endExclusive, "endExclusive is null");
        this.unit = Objects.requireNonNull(unit, "unit is null");
    }

    @Override
    public String toString() {
        return "ExtendedTemporalTile[" + startInclusive + ", " + endExclusive + ") " + unit;
    }

    /**
     * Returns the start of this tile (inclusive).
     *
     * @return the start of this tile
     */
    public T getStartInclusive() {
        return startInclusive;
    }

    /**
     * Returns the end of this tile (exclusive).
     *
     * @return the end of this tile
     */
    public T getEndExclusive() {
        return endExclusive;
    }

    /**
     * Returns the unit that produced this tile.
     *
     * @return the unit of this tile
     */
    public TemporalUnit getUnit() {
        return unit;
    }

    private final T startInclusive;
    private final T endExclusive;
    private final TemporalUnit unit;
}
