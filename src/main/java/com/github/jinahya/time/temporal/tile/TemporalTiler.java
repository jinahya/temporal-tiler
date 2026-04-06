package com.github.jinahya.time.temporal.tile;

import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Decomposes a half-open temporal range {@code [start, end)} into boundary-aligned tiles at a single
 * {@link TemporalUnit} grain.
 *
 * <p>This class provides static methods that partition any temporal range into an ordered sequence of non-overlapping,
 * gap-free tiles. For {@link ChronoUnit}-based tiling, the convenience overload
 * {@link #tile(Temporal, Temporal, ChronoUnit)} supplies a built-in truncation function.
 *
 * @see TemporalTile
 */
public final class TemporalTiler {

    /**
     * Partitions the half-open range {@code [start, end)} into tiles at the specified grain.
     *
     * <p>The returned list contains a sequence of non-overlapping, gap-free tiles in temporal order that together
     * cover the entire input range. Tiles that span a full grain boundary are
     * {@linkplain TemporalTile#isAligned() aligned}; head and tail partial tiles are not.
     *
     * <p>If {@code start} is greater than or equal to {@code end} (empty range), an empty list is returned.
     *
     * @param <T>       the temporal type (e.g., {@link java.time.LocalDate}, {@link java.time.LocalDateTime},
     *                  {@link java.time.Instant})
     * @param start     the inclusive start of the range; must not be {@code null}
     * @param end       the exclusive end of the range; must not be {@code null}
     * @param grain     the grain unit; must not be {@code null}
     * @param truncator a function that truncates a temporal value to the grain boundary at or before it; must not be
     *                  {@code null}
     * @return an unmodifiable list of tiles in temporal order; never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public static <T extends Temporal & Comparable<? super T>>
    List<TemporalTile<T>> tile(final T start, final T end, final TemporalUnit grain,
                               final UnaryOperator<T> truncator) {
        Objects.requireNonNull(start, "start is null");
        Objects.requireNonNull(end, "end is null");
        Objects.requireNonNull(grain, "grain is null");
        Objects.requireNonNull(truncator, "truncator is null");
        if (start.compareTo(end) >= 0) {
            return List.of();
        }
        final var tiles = new ArrayList<TemporalTile<T>>();
        // Truncate start to find the grain boundary at or before start
        final T floorBoundary = truncator.apply(start);
        // The first boundary at or after start
        final T firstBoundary;
        if (floorBoundary.compareTo(start) < 0) {
            // start is not on a boundary -- first boundary is one grain after floor
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
            final var next = (T) grain.addTo(cursor, 1);
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
     * Partitions the half-open range {@code [start, end)} into tiles at the specified {@link ChronoUnit} grain.
     *
     * <p>This is a convenience overload that supplies a built-in truncation function for all standard
     * {@link ChronoUnit} values.
     *
     * @param <T>   the temporal type (e.g., {@link java.time.LocalDate}, {@link java.time.LocalDateTime},
     *              {@link java.time.Instant})
     * @param start the inclusive start of the range; must not be {@code null}
     * @param end   the exclusive end of the range; must not be {@code null}
     * @param grain the {@link ChronoUnit} grain for tiling; must not be {@code null}
     * @return an unmodifiable list of tiles in temporal order; never {@code null}
     * @throws NullPointerException          if any argument is {@code null}
     * @throws UnsupportedOperationException if the grain is not supported by the temporal type
     */
    public static <T extends Temporal & Comparable<? super T>>
    List<TemporalTile<T>> tile(final T start, final T end, final ChronoUnit grain) {
        Objects.requireNonNull(grain, "grain is null");
        return tile(start, end, grain, value -> truncate(value, grain));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Truncates a temporal value to the grain boundary at or before it.
     *
     * <p>For time-based units (NANOS through HALF_DAYS), uses {@link ChronoField#NANO_OF_DAY}
     * with modular arithmetic to zero out sub-fields in one operation. For date-based units, snaps to the start of the
     * period (e.g., 1st of month for MONTHS).
     */
    @SuppressWarnings("unchecked")
    private static <T extends Temporal & Comparable<? super T>>
    T truncate(final T value, final ChronoUnit grain) {
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
                yield (T) result
                        .with(ChronoField.YEAR, year - year % 10)
                        .with(ChronoField.DAY_OF_YEAR, 1);
            }
            case CENTURIES -> {
                final int year = result.get(ChronoField.YEAR);
                yield (T) result
                        .with(ChronoField.YEAR, year - year % 100)
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

    // -----------------------------------------------------------------------------------------------------------------

    private TemporalTiler() {
        throw new AssertionError("instantiation is not allowed");
    }
}
