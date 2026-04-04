package com.github.jinahya.time.temporal.tile;

import java.io.Serializable;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Decomposes a half-open temporal range {@code [start, end)} into boundary-aligned tiles at a single
 * {@link TemporalUnit} grain.
 *
 * <p>Each tiler is configured with a grain (e.g., {@link java.time.temporal.ChronoUnit#MONTHS}), a start (inclusive),
 * and an end (exclusive) via the {@link Builder}. Calling {@link #tile()} partitions the range into an ordered sequence
 * of non-overlapping, gap-free {@link TemporalTile}s. Tiles that span a full grain boundary are marked as
 * {@linkplain TemporalTile#isAligned() aligned}; head and tail partial tiles are not.
 *
 * <p>This class is abstract. For {@link java.time.temporal.ChronoUnit}-based tiling, use {@link ChronoTiler}.
 *
 * <p>To implement a custom tiler for a different {@link TemporalUnit}, subclass this class and provide:
 * <ul>
 *     <li>{@link #truncate(Temporal)} &mdash; boundary truncation for the unit type</li>
 *     <li>{@link #newTile(Temporal, Temporal, TemporalUnit, boolean)} &mdash; tile factory</li>
 *     <li>A concrete {@link Builder} subclass with a {@link Builder#build()} implementation</li>
 * </ul>
 *
 * <p>Instances are immutable and safe for use by multiple threads.
 *
 * @param <T> the temporal type (e.g., {@link java.time.LocalDate}, {@link java.time.LocalDateTime},
 *            {@link java.time.Instant})
 * @param <U> the temporal unit type (e.g., {@link java.time.temporal.ChronoUnit})
 * @see TemporalTile
 * @see ChronoTiler
 */
public abstract class TemporalTiler<T extends Temporal & Comparable<? super T> & Serializable, U extends TemporalUnit & Serializable> {

    // --------------------------------------------------------------------------------------------------------- Builder

    /**
     * Abstract builder for creating {@link TemporalTiler} instances.
     *
     * <p>Concrete subclasses must implement {@link #build()} to return the appropriate tiler type.
     * For {@link java.time.temporal.ChronoUnit}-based tiling, use {@link ChronoTiler.Builder}.
     *
     * @param <T> the temporal type
     * @param <U> the temporal unit type
     */
    public abstract static class Builder<T extends Temporal & Comparable<? super T> & Serializable,
            U extends TemporalUnit & Serializable> {

        // -------------------------------------------------------------------------------------------------------------

        /**
         * Creates a new instance.
         */
        protected Builder() {
            super();
        }

        // -------------------------------------------------------------------------------------------------------------

        /**
         * Builds a new {@link TemporalTiler} with the configured start, end, and grain.
         *
         * <p>Subclasses must override this method to return the appropriate concrete tiler.
         *
         * @return a new tiler instance
         * @throws IllegalStateException if {@link #start(Temporal) start}, {@link #end(Temporal) end}, or
         *                               {@link #grain(TemporalUnit) grain} has not been set
         */
        public abstract TemporalTiler<T, U> build();

        /**
         * Sets the start of the range (inclusive).
         *
         * @param start the start of the range; must not be {@code null}
         * @return this builder for chaining
         * @throws NullPointerException if {@code start} is {@code null}
         */
        public Builder<T, U> start(final T start) {
            this.start = Objects.requireNonNull(start, "start is null");
            return this;
        }

        /**
         * Sets the end of the range (exclusive).
         *
         * @param end the end of the range; must not be {@code null}
         * @return this builder for chaining
         * @throws NullPointerException if {@code end} is {@code null}
         */
        public Builder<T, U> end(final T end) {
            this.end = Objects.requireNonNull(end, "end is null");
            return this;
        }

        /**
         * Sets the grain unit for the tiler.
         *
         * @param grain the grain unit; must not be {@code null}
         * @return this builder for chaining
         * @throws NullPointerException if {@code grain} is {@code null}
         */
        public Builder<T, U> grain(final U grain) {
            this.grain = Objects.requireNonNull(grain, "grain is null");
            return this;
        }

        // -------------------------------------------------------------------------------------------------------------
        T start;

        T end;

        U grain;
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new tiler from the given builder.
     *
     * @param builder the builder; must not be {@code null}
     */
    TemporalTiler(final Builder<T, U> builder) {
        super();
        this.start = builder.start;
        this.end = builder.end;
        this.grain = builder.grain;
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new tile with the given parameters.
     *
     * <p>Subclasses implement this to return the appropriate concrete {@link TemporalTile} subtype.
     *
     * @param start   the inclusive start of the tile's range
     * @param end     the exclusive end of the tile's range
     * @param grain   the grain unit
     * @param aligned whether the tile is boundary-aligned
     * @return a new tile instance
     */
    protected abstract TemporalTile<T, U> newTile(T start, T end, U grain, boolean aligned);

    /**
     * Partitions the half-open range {@code [start, end)} into tiles at this tiler's grain.
     *
     * <p>The returned list contains a sequence of non-overlapping, gap-free tiles in temporal order that together
     * cover the entire input range. Tiles that span a full grain boundary are
     * {@linkplain TemporalTile#isAligned() aligned}; head and tail partial tiles are not.
     *
     * <p>If {@code start} is greater than or equal to {@code end} (empty range),
     * an empty list is returned.
     *
     * @return an unmodifiable list of tiles in temporal order; never {@code null}
     * @throws UnsupportedOperationException if the grain is not supported by the temporal type
     */
    public List<TemporalTile<T, U>> tile() {
        if (start.compareTo(end) >= 0) {
            return List.of();
        }
        final var tiles = new ArrayList<TemporalTile<T, U>>();
        // Truncate start to find the grain boundary at or before start
        final T floorBoundary = truncate(start);
        // The first boundary at or after start
        final T firstBoundary;
        if (floorBoundary.compareTo(start) < 0) {
            // start is not on a boundary — first boundary is one grain after floor
            firstBoundary = (T) grain.addTo(floorBoundary, 1);
        } else {
            // start is on a boundary
            firstBoundary = start;
        }
        // Head partial: [start, firstBoundary) if start is not on a boundary
        if (firstBoundary.compareTo(start) > 0) {
            final T headEnd = firstBoundary.compareTo(end) <= 0 ? firstBoundary : end;
            tiles.add(newTile(start, headEnd, grain, false));
            if (headEnd.compareTo(end) >= 0) {
                return Collections.unmodifiableList(tiles);
            }
        }
        // Aligned tiles from firstBoundary onward
        T cursor = firstBoundary;
        while (cursor.compareTo(end) < 0) {
            final T next = (T) grain.addTo(cursor, 1);
            if (next.compareTo(end) <= 0) {
                tiles.add(newTile(cursor, next, grain, true));
            } else {
                // Tail partial
                tiles.add(newTile(cursor, end, grain, false));
            }
            cursor = next;
        }
        return Collections.unmodifiableList(tiles);
    }

    /**
     * Truncates a temporal value to the grain boundary at or before it.
     *
     * <p>Subclasses implement this to define how boundary alignment is computed for their specific
     * {@link TemporalUnit} type.
     *
     * @param value the temporal value to truncate
     * @return the grain boundary at or before {@code value}
     */
    protected abstract T truncate(T value);

    // ----------------------------------------------------------------------------------------------------------- start

    /**
     * Returns the start of the range (inclusive).
     *
     * @return the start of the range; never {@code null}
     */
    public T getStart() {
        return start;
    }

    // ------------------------------------------------------------------------------------------------------------- end

    /**
     * Returns the end of the range (exclusive).
     *
     * @return the end of the range; never {@code null}
     */
    public T getEnd() {
        return end;
    }

    // ----------------------------------------------------------------------------------------------------------- grain

    /**
     * Returns the {@link TemporalUnit} grain of this tiler.
     *
     * @return the grain of this tiler
     */
    public U getGrain() {
        return grain;
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * The start of the range (inclusive).
     */
    private final T start;

    /**
     * The end of the range (exclusive).
     */
    private final T end;

    /**
     * The grain unit for this tiler.
     */
    private final U grain;
}
