package com.github.jinahya.time.temporal.tile;

import java.io.Serializable;
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
 * <p>This class provides a single static method that partitions any temporal range into an ordered sequence of
 * non-overlapping, gap-free tiles. The caller supplies a {@code truncator} function that defines how to snap a temporal
 * value to the grain boundary at or before it, and a {@code tileFactory} that creates tile instances.
 *
 * <p>For {@link java.time.temporal.ChronoUnit}-based tiling, use the convenience methods in {@link ChronoTiler}.
 *
 * @see TemporalTile
 * @see ChronoTiler
 */
public final class TemporalTiler {

    /**
     * A factory for creating {@link TemporalTile} instances.
     *
     * @param <T> the temporal type
     * @param <U> the temporal unit type
     */
    @FunctionalInterface
    public interface TileFactory<
            T extends Temporal & Comparable<? super T> & Serializable,
            U extends TemporalUnit
            > {

        /**
         * Creates a new tile.
         *
         * @param start   the inclusive start of the range
         * @param end     the exclusive end of the range
         * @param grain   the grain unit
         * @param aligned whether this tile is boundary-aligned
         * @return a new tile instance
         */
        TemporalTile<T, U> create(T start, T end, U grain, boolean aligned);
    }

    /**
     * Partitions the half-open range {@code [start, end)} into tiles at the specified grain.
     *
     * <p>The returned list contains a sequence of non-overlapping, gap-free tiles in temporal order that together
     * cover the entire input range. Tiles that span a full grain boundary are
     * {@linkplain TemporalTile#isAligned() aligned}; head and tail partial tiles are not.
     *
     * <p>If {@code start} is greater than or equal to {@code end} (empty range), an empty list is returned.
     *
     * @param <T>         the temporal type (e.g., {@link java.time.LocalDate}, {@link java.time.LocalDateTime},
     *                    {@link java.time.Instant})
     * @param <U>         the temporal unit type (e.g., {@link java.time.temporal.ChronoUnit})
     * @param start       the inclusive start of the range; must not be {@code null}
     * @param end         the exclusive end of the range; must not be {@code null}
     * @param grain       the grain unit; must not be {@code null}
     * @param truncator   a function that truncates a temporal value to the grain boundary at or before it; must not be
     *                    {@code null}
     * @param tileFactory a factory for creating tile instances; must not be {@code null}
     * @return an unmodifiable list of tiles in temporal order; never {@code null}
     * @throws NullPointerException          if any argument is {@code null}
     * @throws UnsupportedOperationException if the grain is not supported by the temporal type
     */
    public static <
            T extends Temporal & Comparable<? super T> & Serializable,
            U extends TemporalUnit
            >
    List<TemporalTile<T, U>> tile(final T start, final T end, final U grain, final UnaryOperator<T> truncator,
                                  final TileFactory<T, U> tileFactory) {
        Objects.requireNonNull(start, "start is null");
        Objects.requireNonNull(end, "end is null");
        Objects.requireNonNull(grain, "grain is null");
        Objects.requireNonNull(truncator, "truncator is null");
        Objects.requireNonNull(tileFactory, "tileFactory is null");
        if (start.compareTo(end) >= 0) {
            return List.of();
        }
        final var tiles = new ArrayList<TemporalTile<T, U>>();
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
            tiles.add(tileFactory.create(start, headEnd, grain, false));
            if (headEnd.compareTo(end) >= 0) {
                return Collections.unmodifiableList(tiles);
            }
        }
        // Aligned tiles from firstBoundary onward
        T cursor = firstBoundary;
        while (cursor.compareTo(end) < 0) {
            final T next = (T) grain.addTo(cursor, 1);
            if (next.compareTo(end) <= 0) {
                tiles.add(tileFactory.create(cursor, next, grain, true));
            } else {
                // Tail partial
                tiles.add(tileFactory.create(cursor, end, grain, false));
            }
            cursor = next;
        }
        return Collections.unmodifiableList(tiles);
    }

    // -----------------------------------------------------------------------------------------------------------------

    private TemporalTiler() {
        throw new AssertionError("instantiation is not allowed");
    }
}
