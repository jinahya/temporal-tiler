package com.github.jinahya.time.temporal;

import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Decomposes a half-open temporal range {@code [startInclusive, endExclusive)} into boundary-aligned tiles at a single
 * {@link ChronoUnit} grain.
 *
 * <p>Each tiler is configured with a single grain (e.g., {@link ChronoUnit#MONTHS}). Calling
 * {@link #tile(Temporal, Temporal)} partitions the given range into an ordered sequence of non-overlapping, gap-free
 * {@link TemporalTile}s. Tiles that span a full grain boundary are marked as
 * {@linkplain TemporalTile#isAligned() aligned}; head and tail partial tiles are not.
 *
 * <p>For hierarchical tiling (e.g., year → month → day), create multiple tilers and chain them by
 * further tiling partial tiles:
 * <pre>{@code
 * var monthTiler = TemporalTiler.<LocalDate>of(ChronoUnit.MONTHS);
 * var dayTiler   = TemporalTiler.<LocalDate>of(ChronoUnit.DAYS);
 *
 * monthTiler.tile(startDate, endDate).forEach(tile -> {
 *     if (tile.isAligned()) {
 *         handleMonth(tile);
 *     } else {
 *         dayTiler.tile(tile.getStartInclusive(), tile.getEndExclusive())
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

    /**
     * Creates a new tiler for the specified {@link ChronoUnit} grain.
     *
     * <p>Supported grains range from {@link ChronoUnit#NANOS} through {@link ChronoUnit#MILLENNIA},
     * including {@link ChronoUnit#WEEKS}.
     *
     * @param <T>   the temporal type
     * @param grain the grain unit for tiling
     * @return a new tiler for the specified grain
     * @throws NullPointerException if {@code grain} is {@code null}
     */
    public static <T extends Temporal & Comparable<? super T>> TemporalTiler<T> of(
            final ChronoUnit grain) {
        Objects.requireNonNull(grain, "grain is null");
        return new TemporalTiler<>(grain);
    }

    private TemporalTiler(final ChronoUnit grain) {
        this.grain = grain;
    }

    /**
     * Returns the {@link ChronoUnit} grain of this tiler.
     *
     * @return the grain of this tiler
     */
    public ChronoUnit getGrain() {
        return grain;
    }

    /**
     * Partitions the half-open range {@code [startInclusive, endExclusive)} into tiles at this tiler's grain.
     *
     * <p>The returned list contains a sequence of non-overlapping, gap-free tiles in temporal order that together
     * cover the entire input range. Tiles that span a full grain boundary are
     * {@linkplain TemporalTile#isAligned() aligned}; head and tail partial tiles are not.
     *
     * <p>If {@code startInclusive} is greater than or equal to {@code endExclusive} (empty range),
     * an empty list is returned.
     *
     * @param startInclusive the start of the range (inclusive); must not be {@code null}
     * @param endExclusive   the end of the range (exclusive); must not be {@code null}
     * @return an unmodifiable list of tiles in temporal order; never {@code null}
     * @throws NullPointerException          if {@code startInclusive} or {@code endExclusive} is {@code null}
     * @throws UnsupportedOperationException if the grain is not supported by the temporal type
     */
    @SuppressWarnings("unchecked")
    public List<TemporalTile<T>> tile(final T startInclusive,
                                      final T endExclusive) {
        Objects.requireNonNull(startInclusive, "startInclusive is null");
        Objects.requireNonNull(endExclusive, "endExclusive is null");
        if (startInclusive.compareTo(endExclusive) >= 0) {
            return List.of();
        }
        final var tiles = new ArrayList<TemporalTile<T>>();
        // Truncate start to find the grain boundary at or before start
        final T floorBoundary = truncate(startInclusive);
        // The first boundary at or after start
        final T firstBoundary;
        if (floorBoundary.compareTo(startInclusive) < 0) {
            // start is not on a boundary — first boundary is one grain after floor
            firstBoundary = (T) grain.addTo(floorBoundary, 1);
        } else {
            // start is on a boundary
            firstBoundary = startInclusive;
        }
        // Head partial: [start, firstBoundary) if start is not on a boundary
        if (firstBoundary.compareTo(startInclusive) > 0) {
            final T headEnd = firstBoundary.compareTo(endExclusive) <= 0 ? firstBoundary : endExclusive;
            tiles.add(new TemporalTile<>(startInclusive, headEnd, grain, false));
            if (headEnd.compareTo(endExclusive) >= 0) {
                return Collections.unmodifiableList(tiles);
            }
        }
        // Aligned tiles from firstBoundary onward
        T cursor = firstBoundary;
        while (cursor.compareTo(endExclusive) < 0) {
            final T next = (T) grain.addTo(cursor, 1);
            if (next.compareTo(endExclusive) <= 0) {
                tiles.add(new TemporalTile<>(cursor, next, grain, true));
            } else {
                // Tail partial
                tiles.add(new TemporalTile<>(cursor, endExclusive, grain, false));
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
                yield (T) result.with(ChronoField.YEAR, year - year % 1000)
                        .with(ChronoField.DAY_OF_YEAR, 1);
            }
            default -> throw new UnsupportedOperationException("unsupported grain: " + grain);
        };
    }

    // -----------------------------------------------------------------------------------------------------------------
    private final ChronoUnit grain;
}
