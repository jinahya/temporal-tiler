package com.github.jinahya.time.temporal;

import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Decomposes a half-open temporal range {@code [start, end)} into boundary-aligned tiles at a single {@link ChronoUnit}
 * grain.
 *
 * <p>Each tiler is configured with a grain (e.g., {@link ChronoUnit#MONTHS}), a start (inclusive), and an end
 * (exclusive) via the {@link Builder}. Calling {@link #tile()} partitions the range into an ordered sequence of
 * non-overlapping, gap-free {@link TemporalTile}s. Tiles that span a full grain boundary are marked as
 * {@linkplain TemporalTile#isAligned() aligned}; head and tail partial tiles are not.
 *
 * <p>For hierarchical tiling (e.g., year → month → day), create multiple tilers and chain them by
 * further tiling partial tiles:
 * <pre>{@code
 * var monthTiles = new TemporalTiler.Builder<LocalDate>()
 *     .start(startDate).end(endDate)
 *     .grain(ChronoUnit.MONTHS)
 *     .build().tile();
 *
 * monthTiles.forEach(tile -> {
 *     if (tile.isAligned()) {
 *         handleMonth(tile);
 *     } else {
 *         new TemporalTiler.Builder<LocalDate>()
 *             .start(tile.getStart()).end(tile.getEnd())
 *             .grain(ChronoUnit.DAYS)
 *             .build().tile()
 *             .forEach(dayTile -> handleDay(dayTile));
 *     }
 * });
 * }</pre>
 *
 * <p>Instances are immutable and safe for use by multiple threads.
 *
 * @param <T> the temporal type (e.g., {@link java.time.LocalDate}, {@link java.time.LocalDateTime},
 *            {@link java.time.Instant})
 * @see TemporalTile
 */
public final class TemporalTiler<T extends Temporal & Comparable<? super T>> {

    // --------------------------------------------------------------------------------------------------------- Builder

    /**
     * A builder for creating {@link TemporalTiler} instances.
     *
     * <p>Usage:
     * <pre>{@code
     * List<TemporalTile<LocalDate>> tiles =
     *     new TemporalTiler.Builder<LocalDate>()
     *         .grain(ChronoUnit.MONTHS)
     *         .start(LocalDate.of(2025, 3, 15))
     *         .end(LocalDate.of(2025, 6, 10))
     *         .build()
     *         .tile();
     * }</pre>
     *
     * @param <T> the temporal type
     */
    public static final class Builder<T extends Temporal & Comparable<? super T>> {

        // -------------------------------------------------------------------------------------------------------------
        public Builder() {
            super();
        }

        // -------------------------------------------------------------------------------------------------------------

        /**
         * Builds a new {@link TemporalTiler} with the configured start, end, and grain.
         *
         * @return a new tiler instance
         * @throws IllegalStateException if {@link #start(Temporal) start}, {@link #end(Temporal) end}, or
         *                               {@link #grain(ChronoUnit) grain} has not been set
         */
        public TemporalTiler<T> build() {
            if (start == null) {
                throw new IllegalStateException("start has not been set");
            }
            if (end == null) {
                throw new IllegalStateException("end has not been set");
            }
            if (grain == null) {
                throw new IllegalStateException("grain has not been set");
            }
            return new TemporalTiler<>(this);
        }

        /**
         * Sets the start of the range (inclusive).
         *
         * @param start the start of the range; must not be {@code null}
         * @return this builder for chaining
         * @throws NullPointerException if {@code start} is {@code null}
         */
        public Builder<T> start(final T start) {
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
        public Builder<T> end(final T end) {
            this.end = Objects.requireNonNull(end, "end is null");
            return this;
        }

        /**
         * Sets the grain unit for the tiler.
         *
         * @param grain the {@link ChronoUnit} grain; must not be {@code null}
         * @return this builder for chaining
         * @throws NullPointerException if {@code grain} is {@code null}
         */
        public Builder<T> grain(final ChronoUnit grain) {
            this.grain = Objects.requireNonNull(grain, "grain is null");
            return this;
        }

        // -------------------------------------------------------------------------------------------------------------
        private T start;

        private T end;

        private ChronoUnit grain;
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new tiler for the specified range and grain.
     *
     * <p>This is a convenience factory equivalent to:
     * <pre>{@code
     * new Builder<T>().start(start).end(end).grain(grain).build()
     * }</pre>
     *
     * @param <T>   the temporal type
     * @param start the start of the range (inclusive); must not be {@code null}
     * @param end   the end of the range (exclusive); must not be {@code null}
     * @param grain the {@link ChronoUnit} grain for tiling; must not be {@code null}
     * @return a new tiler configured with the given range and grain
     * @throws NullPointerException if any argument is {@code null}
     * @see #tile(Temporal, Temporal, ChronoUnit)
     */
    public static <T extends Temporal & Comparable<? super T>>
    TemporalTiler<T> of(final T start, final T end, final ChronoUnit grain) {
        Objects.requireNonNull(start, "start is null");
        Objects.requireNonNull(end, "end is null");
        Objects.requireNonNull(grain, "grain is null");
        return new Builder<T>()
                .start(start)
                .end(end)
                .grain(grain)
                .build();
    }

    /**
     * Tiles the specified range at the given grain in a single call.
     *
     * <p>This is a convenience method equivalent to:
     * <pre>{@code
     * of(start, end, grain).tile()
     * }</pre>
     *
     * @param <T>   the temporal type
     * @param start the start of the range (inclusive); must not be {@code null}
     * @param end   the end of the range (exclusive); must not be {@code null}
     * @param grain the {@link ChronoUnit} grain for tiling; must not be {@code null}
     * @return an unmodifiable list of tiles in temporal order; never {@code null}
     * @throws NullPointerException          if any argument is {@code null}
     * @throws UnsupportedOperationException if the grain is not supported by the temporal type
     * @see #of(Temporal, Temporal, ChronoUnit)
     */
    public static <T extends Temporal & Comparable<? super T>>
    List<TemporalTile<T>> tile(final T start, final T end, final ChronoUnit grain) {
        return of(start, end, grain).tile();
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new tiler from the given builder.
     *
     * @param builder the builder; must not be {@code null}
     */
    private TemporalTiler(final Builder<T> builder) {
        super();
        this.start = builder.start;
        this.end = builder.end;
        this.grain = builder.grain;
    }

    // -----------------------------------------------------------------------------------------------------------------

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
    public List<TemporalTile<T>> tile() {
        if (start.compareTo(end) >= 0) {
            return List.of();
        }
        final var tiles = new ArrayList<TemporalTile<T>>();
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
            tiles.add(new TemporalTile<>(start, headEnd, grain, false));
            if (headEnd.compareTo(end) >= 0) {
                return Collections.unmodifiableList(tiles);
            }
        }
        // Aligned tiles from firstBoundary onward
        T cursor = firstBoundary;
        while (cursor.compareTo(end) < 0) {
            final T next = (T) grain.addTo(cursor, 1);
            if (next.compareTo(end) <= 0) {
                tiles.add(new TemporalTile<>(cursor, next, grain, true));
            } else {
                // Tail partial
                tiles.add(new TemporalTile<>(cursor, end, grain, false));
            }
            cursor = next;
        }
        return Collections.unmodifiableList(tiles);
    }

    /**
     * Truncates a temporal value to the grain boundary at or before it.
     *
     * <p>For time-based units (NANOS through HALF_DAYS), uses {@link ChronoField#NANO_OF_DAY}
     * with modular arithmetic to zero out sub-fields in one operation. For date-based units, snaps to the start of the
     * period (e.g., 1st of month for MONTHS).
     *
     * @param value the temporal value to truncate
     * @return the grain boundary at or before {@code value}
     */
    @SuppressWarnings("unchecked")
    private T truncate(final T value) {
        // Time-based grains: truncate via NANO_OF_DAY modular arithmetic
        if (grain.isTimeBased() && value.isSupported(ChronoField.NANO_OF_DAY)) {
            final long nanoOfDay = value.getLong(ChronoField.NANO_OF_DAY);
            final long grainNanos = grain.getDuration().toNanos();
            final long truncated = nanoOfDay - (nanoOfDay % grainNanos);
            return (T) value.with(ChronoField.NANO_OF_DAY, truncated);
        }
        // Date-based grains: zero out time fields first, then snap date fields
        var result = value;
        if (result.isSupported(ChronoField.NANO_OF_DAY)) {
            result = (T) result.with(ChronoField.NANO_OF_DAY, 0L);
        }
        return switch (grain) {
            case DAYS -> result;
            case WEEKS -> {
                final int dayOfWeek = result.get(ChronoField.DAY_OF_WEEK);
                yield (T) result.minus(dayOfWeek - 1, ChronoUnit.DAYS);
            }
            case MONTHS -> (T) result.with(ChronoField.DAY_OF_MONTH, 1);
            case YEARS -> (T) result.with(ChronoField.DAY_OF_YEAR, 1);
            case DECADES -> {
                final int year = result.get(ChronoField.YEAR);
                yield (T) result.with(ChronoField.YEAR, year - year % 10)
                        .with(ChronoField.DAY_OF_YEAR, 1);
            }
            case CENTURIES -> {
                final int year = result.get(ChronoField.YEAR);
                yield (T) result.with(ChronoField.YEAR, year - year % 100)
                        .with(ChronoField.DAY_OF_YEAR, 1);
            }
            case MILLENNIA -> {
                final int year = result.get(ChronoField.YEAR);
                yield (T) result
                        .with(ChronoField.YEAR, year - year % 1000)
                        .with(ChronoField.DAY_OF_YEAR, 1);
            }
            default -> throw new UnsupportedOperationException("unsupported grain: " + grain);
        };
    }
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
     * Returns the {@link ChronoUnit} grain of this tiler.
     *
     * @return the grain of this tiler
     */
    public ChronoUnit getGrain() {
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
    private final ChronoUnit grain;
}
