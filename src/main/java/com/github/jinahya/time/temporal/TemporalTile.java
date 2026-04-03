package com.github.jinahya.time.temporal;

import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.stream.Stream;

/**
 * A single tile: a half-open temporal range {@code [startInclusive, endExclusive)} with metadata.
 *
 * <p>Tiles are produced by {@link TemporalTiler#tile(Temporal, Temporal)} and can be further
 * decomposed via {@link #decompose()}.
 *
 * @param <T> the temporal type
 */
public final class TemporalTile<T extends Temporal & Comparable<? super T>> {

    TemporalTile(final T startInclusive, final T endExclusive,
                 final ChronoUnit grain, final boolean aligned, final int depth,
                 final TemporalTiler<T> tiler) {
        this.startInclusive = startInclusive;
        this.endExclusive = endExclusive;
        this.grain = grain;
        this.aligned = aligned;
        this.depth = depth;
        this.tiler = tiler;
    }

    /**
     * Returns the start of this tile (inclusive).
     */
    public T getStartInclusive() {
        return startInclusive;
    }

    /**
     * Returns the end of this tile (exclusive).
     */
    public T getEndExclusive() {
        return endExclusive;
    }

    /**
     * Returns the grain that produced this tile.
     */
    public ChronoUnit getGrain() {
        return grain;
    }

    /**
     * Returns {@code true} if this tile is boundary-aligned (full grain).
     */
    public boolean isAligned() {
        return aligned;
    }

    /**
     * Returns the depth of this tile in the grain hierarchy.
     * {@code 0} is the widest grain, incrementing toward the narrowest.
     */
    public int getDepth() {
        return depth;
    }

    /**
     * Decomposes this tile into finer grains using the remaining grain levels below this tile's grain.
     *
     * <p>For example, given a tiler with grains {@code [YEARS, MONTHS, DAYS]} and a MONTHS tile,
     * this method tiles the month's range with {@code [DAYS]}.
     *
     * <p>Returns an empty stream if this tile is already at the narrowest grain.
     *
     * @return stream of finer tiles in temporal order
     */
    public Stream<TemporalTile<T>> decompose() {
        return tiler.decompose(this);
    }

    private final T startInclusive;
    private final T endExclusive;
    private final ChronoUnit grain;
    private final boolean aligned;
    private final int depth;
    private final TemporalTiler<T> tiler;
}
