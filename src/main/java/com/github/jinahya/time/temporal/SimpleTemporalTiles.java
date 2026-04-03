package com.github.jinahya.time.temporal;

import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Utility class for producing flat, uniform-grain tiles from a temporal range.
 *
 * <p>Unlike {@link TemporalTiler}, this class partitions a range using a single {@link TemporalUnit} with no
 * hierarchical decomposition.
 */
public final class SimpleTemporalTiles {

    /**
     * Partitions the half-open range {@code [startInclusive, endExclusive)} into tiles of the given unit.
     *
     * <p>Each tile covers exactly one unit, except possibly the last tile which may be shorter if the range does not
     * divide evenly by the unit.
     *
     * @param <T>            the temporal type
     * @param startInclusive range start (inclusive)
     * @param endExclusive   range end (exclusive)
     * @param temporalUnit   the temporal unit for each tile
     * @return an unmodifiable list of tiles in temporal order
     * @throws IllegalArgumentException if {@code startInclusive} is not before {@code endExclusive}
     */
    @SuppressWarnings("unchecked")
    public static <T extends Temporal & Comparable<? super T>>
    List<SimpleTemporalTile<T>> tile(final T startInclusive,
                                     final T endExclusive,
                                     final TemporalUnit temporalUnit) {
        Objects.requireNonNull(startInclusive, "startInclusive is null");
        Objects.requireNonNull(endExclusive, "endExclusive is null");
        Objects.requireNonNull(temporalUnit, "unit is null");
        if (startInclusive.compareTo(endExclusive) >= 0) {
            throw new IllegalArgumentException(
                    "startInclusive (" + startInclusive + ") must be before endExclusive (" + endExclusive + ")"
            );
        }
        final var tiles = new ArrayList<SimpleTemporalTile<T>>();
        for (var c = startInclusive; c.compareTo(endExclusive) < 0; ) {
            var next = (T) c.plus(1L, temporalUnit);
            if (next.compareTo(endExclusive) > 0) {
                next = endExclusive;
            }
            tiles.add(new SimpleTemporalTile<>(c, next, temporalUnit));
            c = next;
        }
        return Collections.unmodifiableList(tiles);
    }

    // -----------------------------------------------------------------------------------------------------------------
    private SimpleTemporalTiles() {
        throw new AssertionError("no instances");
    }
}
