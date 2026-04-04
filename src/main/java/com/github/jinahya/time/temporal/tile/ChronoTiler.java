package com.github.jinahya.time.temporal.tile;

import java.io.Serializable;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Objects;

/**
 * A {@link TemporalTiler} whose grain is a {@link ChronoUnit}.
 *
 * <p>This is the standard concrete implementation for {@code TemporalTiler<T, ChronoUnit>}, reducing verbosity at
 * usage sites.
 *
 * <p>Usage:
 * <pre>{@code
 * var tiles = new ChronoTiler.Builder<LocalDate>()
 *     .start(LocalDate.of(2025, 3, 15))
 *     .end(LocalDate.of(2025, 6, 10))
 *     .grain(ChronoUnit.MONTHS)
 *     .build()
 *     .tile();
 * }</pre>
 *
 * @param <T> the temporal type (e.g., {@link java.time.LocalDate}, {@link java.time.LocalDateTime},
 *            {@link java.time.Instant})
 * @see ChronoTile
 */
public final class ChronoTiler<T extends Temporal & Comparable<? super T> & Serializable>
        extends TemporalTiler<T, ChronoUnit> {

    // --------------------------------------------------------------------------------------------------------- Builder

    /**
     * A builder for creating {@link ChronoTiler} instances.
     *
     * @param <T> the temporal type
     */
    public static final class Builder<T extends Temporal & Comparable<? super T> & Serializable>
            extends TemporalTiler.Builder<T, ChronoUnit> {

        /**
         * Creates a new instance.
         */
        public Builder() {
            super();
        }

        // -------------------------------------------------------------------------------------------------------------
        @Override
        public ChronoTiler<T> build() {
            if (super.start == null) {
                throw new IllegalStateException("start has not been set");
            }
            if (super.end == null) {
                throw new IllegalStateException("end has not been set");
            }
            if (super.grain == null) {
                throw new IllegalStateException("grain has not been set");
            }
            return new ChronoTiler<>(this);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new chrono tiler for the specified range and grain.
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
     * @return a new chrono tiler configured with the given range and grain
     * @throws NullPointerException if any argument is {@code null}
     * @see #tile(Temporal, Temporal, ChronoUnit)
     */
    public static <T extends Temporal & Comparable<? super T> & Serializable>
    ChronoTiler<T> of(final T start, final T end, final ChronoUnit grain) {
        Objects.requireNonNull(start, "start is null");
        Objects.requireNonNull(end, "end is null");
        Objects.requireNonNull(grain, "grain is null");
        final var builder = new Builder<T>();
        builder.start(start);
        builder.end(end);
        builder.grain(grain);
        return builder.build();
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
    public static <T extends Temporal & Comparable<? super T> & Serializable>
    List<TemporalTile<T, ChronoUnit>> tile(final T start, final T end, final ChronoUnit grain) {
        return of(start, end, grain).tile();
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new chrono tiler from the given builder.
     *
     * @param builder the builder
     */
    private ChronoTiler(final Builder<T> builder) {
        super(builder);
    }

    // -----------------------------------------------------------------------------------------------------------------

    @Override
    protected ChronoTile<T> newTile(final T start, final T end, final ChronoUnit grain,
                                    final boolean aligned) {
        return new ChronoTile<>(start, end, grain, aligned);
    }

    /**
     * {@inheritDoc}
     *
     * <p>For time-based units (NANOS through HALF_DAYS), uses {@link ChronoField#NANO_OF_DAY}
     * with modular arithmetic to zero out sub-fields in one operation. For date-based units, snaps to the start of the
     * period (e.g., 1st of month for MONTHS).
     */
    @SuppressWarnings("unchecked")
    @Override
    protected T truncate(final T value) {
        final var grain = getGrain();
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
}
