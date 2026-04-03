package com.github.jinahya.time.temporal;

import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;

/**
 * A single tile representing a half-open temporal range {@code [startInclusive, endExclusive)} at a specific
 * {@link ChronoUnit} grain.
 *
 * <p>A tile is either <em>boundary-aligned</em> (its start and end both fall on natural grain boundaries, spanning
 * exactly one full grain) or <em>partial</em> (a head or tail fragment that does not span a full grain).
 *
 * <p>Instances are created by {@link TemporalTiler#tile(Temporal, Temporal)} and are immutable.
 *
 * @param <T> the temporal type (e.g., {@link java.time.LocalDate}, {@link java.time.LocalDateTime},
 *            {@link java.time.Instant})
 * @see TemporalTiler
 */
public final class TemporalTile<T extends Temporal & Comparable<? super T>> {

    TemporalTile(final T startInclusive, final T endExclusive,
                 final ChronoUnit grain, final boolean aligned) {
        this.startInclusive = startInclusive;
        this.endExclusive = endExclusive;
        this.grain = grain;
        this.aligned = aligned;
    }

    /**
     * Returns the inclusive start of this tile's range.
     *
     * @return the start of this tile (inclusive)
     */
    public T getStartInclusive() {
        return startInclusive;
    }

    /**
     * Returns the exclusive end of this tile's range.
     *
     * @return the end of this tile (exclusive)
     */
    public T getEndExclusive() {
        return endExclusive;
    }

    /**
     * Returns the {@link ChronoUnit} grain of this tile.
     *
     * @return the grain of this tile
     */
    public ChronoUnit getGrain() {
        return grain;
    }

    /**
     * Returns {@code true} if this tile is boundary-aligned, meaning its start and end both fall on natural grain
     * boundaries and it spans exactly one full grain unit.
     *
     * <p>Partial tiles (head or tail fragments) return {@code false}.
     *
     * @return {@code true} if this tile is boundary-aligned; {@code false} if it is a partial tile
     */
    public boolean isAligned() {
        return aligned;
    }

    private final T startInclusive;

    private final T endExclusive;

    private final ChronoUnit grain;

    private final boolean aligned;
}
