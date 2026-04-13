package com.github.jinahya.time.temporal.tile;

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
 * <p>This class provides a static method that partitions any temporal range into an ordered sequence of
 * non-overlapping, gap-free tiles.
 *
 * @see TemporalTile
 */
public final class TemporalTiler {

    /**
     * Partitions the half-open range {@code [start, end)} into tiles at the specified {@link ChronoUnit} grain.
     *
     * <p>The returned list contains a sequence of non-overlapping, gap-free tiles in temporal order that together
     * cover the entire input range. Tiles that span a full grain boundary are
     * {@linkplain TemporalTile#aligned() aligned}; head and tail partial tiles are not.
     *
     * <p>If {@code start} is greater than or equal to {@code end} (empty range), an empty list is returned.
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
    @SuppressWarnings("unchecked")
    public static <T extends Temporal & Comparable<? super T>>
    List<TemporalTile<T>> tile(final T start, final T end, final ChronoUnit grain) {
        Objects.requireNonNull(start, "start is null");
        Objects.requireNonNull(end, "end is null");
        Objects.requireNonNull(grain, "grain is null");
        if (start.compareTo(end) >= 0) {
            return List.of();
        }
        final var tiles = new ArrayList<TemporalTile<T>>();
        // Truncate start to find the grain boundary at or before start
        final var floorBoundary = truncate(start, grain);
        // The first boundary at or after start
        final var firstBoundary = floorBoundary.compareTo(start) < 0
                ? (T) floorBoundary.plus(1, grain)
                : start;
        // Head partial: [start, firstBoundary) if start is not on a boundary
        if (firstBoundary.compareTo(start) > 0) {
            final var headEnd = firstBoundary.compareTo(end) <= 0 ? firstBoundary : end;
            tiles.add(new TemporalTile<>(start, headEnd, grain, false));
            if (headEnd.compareTo(end) >= 0) {
                return Collections.unmodifiableList(tiles);
            }
        }
        // Aligned tiles from firstBoundary onward
        var cursor = firstBoundary;
        while (cursor.compareTo(end) < 0) {
            final var next = (T) cursor.plus(1, grain);
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
     * Partitions the half-open range of the given tile into sub-tiles at the specified {@link ChronoUnit} grain.
     *
     * <p>This is a convenience overload equivalent to
     * {@code tile(tile.start(), tile.end(), grain)}.
     *
     * @param <T>   the temporal type
     * @param tile  the tile whose range to partition; must not be {@code null}
     * @param grain the {@link ChronoUnit} grain for tiling; must not be {@code null}
     * @return an unmodifiable list of sub-tiles in temporal order; never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     * @see #tile(Temporal, Temporal, ChronoUnit)
     */
    public static <T extends Temporal & Comparable<? super T>>
    List<TemporalTile<T>> tile(final TemporalTile<T> tile, final ChronoUnit grain) {
        Objects.requireNonNull(tile, "tile is null");
        return tile(tile.start(), tile.end(), grain);
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
            final var nanoOfDay = value.getLong(ChronoField.NANO_OF_DAY);
            final var grainNanos = grain.getDuration().toNanos();
            final var truncated = nanoOfDay - (nanoOfDay % grainNanos);
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
                final var dayOfWeek = result.get(ChronoField.DAY_OF_WEEK);
                yield (T) result.minus(dayOfWeek - 1, ChronoUnit.DAYS);
            }
            case MONTHS -> result.isSupported(ChronoField.DAY_OF_MONTH)
                    ? (T) result.with(ChronoField.DAY_OF_MONTH, 1)
                    : result;
            case YEARS -> result.isSupported(ChronoField.DAY_OF_YEAR)
                    ? (T) result.with(ChronoField.DAY_OF_YEAR, 1)
                    : result.isSupported(ChronoField.MONTH_OF_YEAR)
                      ? (T) result.with(ChronoField.MONTH_OF_YEAR, 1)
                      : result;
            case DECADES -> {
                final var year = result.get(ChronoField.YEAR);
                var r = (T) result.with(ChronoField.YEAR, year - Math.floorMod(year, 10));
                if (r.isSupported(ChronoField.DAY_OF_YEAR)) {
                    r = (T) r.with(ChronoField.DAY_OF_YEAR, 1);
                } else if (r.isSupported(ChronoField.MONTH_OF_YEAR)) {
                    r = (T) r.with(ChronoField.MONTH_OF_YEAR, 1);
                }
                yield r;
            }
            case CENTURIES -> {
                final var year = result.get(ChronoField.YEAR);
                var r = (T) result.with(ChronoField.YEAR, year - Math.floorMod(year, 100));
                if (r.isSupported(ChronoField.DAY_OF_YEAR)) {
                    r = (T) r.with(ChronoField.DAY_OF_YEAR, 1);
                } else if (r.isSupported(ChronoField.MONTH_OF_YEAR)) {
                    r = (T) r.with(ChronoField.MONTH_OF_YEAR, 1);
                }
                yield r;
            }
            case MILLENNIA -> {
                final var year = result.get(ChronoField.YEAR);
                var r = (T) result.with(ChronoField.YEAR, year - Math.floorMod(year, 1000));
                if (r.isSupported(ChronoField.DAY_OF_YEAR)) {
                    r = (T) r.with(ChronoField.DAY_OF_YEAR, 1);
                } else if (r.isSupported(ChronoField.MONTH_OF_YEAR)) {
                    r = (T) r.with(ChronoField.MONTH_OF_YEAR, 1);
                }
                yield r;
            }
            default -> throw new UnsupportedOperationException("unsupported grain: " + grain);
        };
    }

    // -----------------------------------------------------------------------------------------------------------------

    private TemporalTiler() {
        throw new AssertionError("instantiation is not allowed");
    }
}
