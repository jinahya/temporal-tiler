package com.github.jinahya.time.temporal;

import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.stream.Stream;

/**
 * Decomposes temporal ranges into hierarchical, boundary-aligned, non-overlapping tiles.
 *
 * <p>A tiler is configured with an explicit list of {@link ChronoUnit} grains from widest to
 * narrowest. The user controls exactly which grain levels to tile at. Aligned tiles stay at their
 * grain level; only partial (unaligned) head/tail tiles are decomposed further.
 *
 * <p>Usage:
 * <pre>{@code
 * var tiler = TemporalTiler.<LocalDate>of(ChronoUnit.YEARS, ChronoUnit.MONTHS, ChronoUnit.DAYS);
 * tiler.tile(LocalDate.of(2025, 3, 15), LocalDate.of(2027, 6, 10))
 *      .forEach(tile -> System.out.println(tile.getGrain() + ": " + tile.getStartInclusive()));
 * }</pre>
 *
 * @param <T> the temporal type (e.g., {@code LocalDate}, {@code LocalDateTime}, {@code ZonedDateTime})
 */
public final class TemporalTiler<T extends Temporal & Comparable<? super T>> {

    // ------------------------------------------------------------------------------------------------------ factory

    /**
     * Creates a tiler with the given grain hierarchy.
     *
     * <p>Each consecutive pair must be reachable in the {@link ChronoUnit} decomposition graph
     * (i.e., the wider unit can cleanly decompose into the narrower unit).
     *
     * @param grains grains from widest to narrowest
     * @param <T>    the temporal type
     * @return a new tiler
     * @throws IllegalArgumentException if grains is empty or any consecutive pair is invalid
     */
    @SafeVarargs
    public static <T extends Temporal & Comparable<? super T>> TemporalTiler<T> of(
            final ChronoUnit... grains) {
        return of(List.of(grains));
    }

    /**
     * Creates a tiler with the given grain hierarchy.
     *
     * @param grains grains from widest to narrowest
     * @param <T>    the temporal type
     * @return a new tiler
     * @throws IllegalArgumentException if grains is empty or any consecutive pair is invalid
     */
    public static <T extends Temporal & Comparable<? super T>> TemporalTiler<T> of(
            final List<ChronoUnit> grains) {
        if (grains.isEmpty()) {
            throw new IllegalArgumentException("grains is empty");
        }
        if (!TemporalTilerUtils.isValidGrainSequence(grains)) {
            throw new IllegalArgumentException("invalid grain sequence: " + grains);
        }
        return new TemporalTiler<>(grains);
    }

    // --------------------------------------------------------------------------------------------------- constructor

    private TemporalTiler(final List<ChronoUnit> grains) {
        this.grains = List.copyOf(grains);
    }

    // ------------------------------------------------------------------------------------------------------ accessor

    /**
     * Returns the grain hierarchy (widest to narrowest).
     *
     * @return immutable list of grains
     */
    public List<ChronoUnit> getGrains() {
        return grains;
    }

    // --------------------------------------------------------------------------------------------------------- tile

    /**
     * Tiles the range {@code [startInclusive, endExclusive)} and returns a mixed-grain stream
     * in temporal order.
     *
     * <p>Aligned tiles stay at their grain level; only partial (unaligned) head/tail tiles
     * are decomposed into the next narrower grain. The output is non-overlapping and gap-free.
     *
     * @param startInclusive range start (inclusive)
     * @param endExclusive   range end (exclusive)
     * @return stream of tiles in temporal order, each tagged with its grain and alignment
     */
    public Stream<TemporalTile<T>> tile(final T startInclusive,
                                        final T endExclusive) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    // ----------------------------------------------------------------------------------------------------- decompose

    /**
     * Decomposes a tile into finer grains using the remaining grain levels below the tile's grain.
     *
     * <p>For example, given a tiler with grains {@code [YEARS, MONTHS, DAYS]} and a MONTHS tile,
     * this method tiles the month's range with {@code [DAYS]}.
     *
     * <p>Returns an empty stream if the tile is already at the narrowest grain.
     *
     * @param tile the tile to decompose
     * @return stream of finer tiles in temporal order
     */
    public Stream<TemporalTile<T>> decompose(final TemporalTile<T> tile) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    // -------------------------------------------------------------------------------------------------------- field

    private final List<ChronoUnit> grains;
}
