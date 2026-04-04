package com.github.jinahya.time.temporal.tile;

import java.io.Serializable;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Objects;

/**
 * Convenience methods for tiling temporal ranges with a {@link ChronoUnit} grain.
 *
 * <p>This class provides a static {@link #tile(Temporal, Temporal, ChronoUnit)} method that delegates to
 * {@link TemporalTiler#tile(Temporal, Temporal, java.time.temporal.TemporalUnit, java.util.function.UnaryOperator,
 * TemporalTiler.TileFactory)} with a built-in truncation function for all standard {@link ChronoUnit} values.
 *
 * <p>Usage:
 * <pre>{@code
 * var tiles = ChronoTiler.tile(
 *     LocalDate.of(2025, 3, 15),
 *     LocalDate.of(2025, 6, 10),
 *     ChronoUnit.MONTHS
 * );
 * }</pre>
 *
 * @see TemporalTiler
 * @see TemporalTile
 * @see ChronoTile
 */
public final class ChronoTiler {

    /**
     * Tiles the specified range at the given {@link ChronoUnit} grain.
     *
     * <p>This is a convenience method that delegates to
     * {@link TemporalTiler#tile(Temporal, Temporal, java.time.temporal.TemporalUnit, java.util.function.UnaryOperator,
     * TemporalTiler.TileFactory)} with a built-in truncation function for {@link ChronoUnit} and {@link ChronoTile} as
     * the tile type.
     *
     * @param <T>   the temporal type (e.g., {@link java.time.LocalDate}, {@link java.time.LocalDateTime},
     *              {@link java.time.Instant})
     * @param start the start of the range (inclusive); must not be {@code null}
     * @param end   the end of the range (exclusive); must not be {@code null}
     * @param grain the {@link ChronoUnit} grain for tiling; must not be {@code null}
     * @return an unmodifiable list of tiles in temporal order; never {@code null}
     * @throws NullPointerException          if any argument is {@code null}
     * @throws UnsupportedOperationException if the grain is not supported by the temporal type
     */
    public static <
            T extends Temporal & Comparable<? super T> & Serializable
            >
    List<TemporalTile<T, ChronoUnit>> tile(final T start, final T end, final ChronoUnit grain) {
        Objects.requireNonNull(start, "start is null");
        Objects.requireNonNull(end, "end is null");
        Objects.requireNonNull(grain, "grain is null");
        return TemporalTiler.tile(
                start,
                end,
                grain,
                value -> truncate(value, grain),
                ChronoTile::new
        );
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
    private static <
            T extends Temporal & Comparable<? super T> & Serializable
            >
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
            result = (T) result
                    .with(ChronoField.NANO_OF_DAY, 0L);
        }
        return switch (grain) {
            case DAYS -> result;
            case WEEKS -> {
                final int dayOfWeek = result.get(ChronoField.DAY_OF_WEEK);
                yield (T) result
                        .minus(dayOfWeek - 1, ChronoUnit.DAYS);
            }
            case MONTHS -> (T) result
                    .with(ChronoField.DAY_OF_MONTH, 1);
            case YEARS -> (T) result
                    .with(ChronoField.DAY_OF_YEAR, 1);
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

    private ChronoTiler() {
        throw new AssertionError("instantiation is not allowed");
    }
}
