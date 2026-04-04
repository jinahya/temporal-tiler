package com.github.jinahya.time.temporal;

import java.io.Serial;
import java.io.Serializable;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.Objects;

/**
 * A single tile representing a half-open temporal range {@code [start, end)} at a specific {@link ChronoUnit} grain.
 *
 * <p>A tile is either <em>boundary-aligned</em> (its start and end both fall on natural grain boundaries, spanning
 * exactly one full grain) or <em>partial</em> (a head or tail fragment that does not span a full grain).
 *
 * <p>Instances are created by {@link TemporalTiler#tile()} and are immutable.
 *
 * @param <T> the temporal type (e.g., {@link java.time.LocalDate}, {@link java.time.LocalDateTime},
 *            {@link java.time.Instant})
 * @see TemporalTiler
 */
public final class TemporalTile<T extends Temporal & Comparable<? super T>>
        implements Serializable {

    @Serial
    private static final long serialVersionUID = -2680380686049055769L;

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new tile.
     *
     * @param start   the inclusive start of the range
     * @param end     the exclusive end of the range
     * @param grain   the grain unit
     * @param aligned whether this tile is boundary-aligned
     */
    TemporalTile(final T start, final T end, final ChronoUnit grain, final boolean aligned) {
        super();
        this.start = Objects.requireNonNull(start, "start is null");
        this.end = Objects.requireNonNull(end, "end is null");
        this.grain = grain;
        this.aligned = aligned;
    }

    // ------------------------------------------------------------------------------------------------ java.lang.Object

    @Override
    public String toString() {
        return super.toString() + '{' +
               "start=" + start +
               ",end=" + end +
               ",grain=" + grain +
               ",aligned=" + aligned +
               '}';
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final var that = (TemporalTile<?>) obj;
        return aligned == that.aligned &&
               start.equals(that.start) &&
               end.equals(that.end) &&
               grain == that.grain;
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end, grain, aligned);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Returns the inclusive start of this tile's range.
     *
     * @return the start of this tile (inclusive)
     */
    public T getStart() {
        return start;
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Returns the exclusive end of this tile's range.
     *
     * @return the end of this tile (exclusive)
     */
    public T getEnd() {
        return end;
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Returns the {@link ChronoUnit} grain of this tile.
     *
     * @return the grain of this tile
     */
    public ChronoUnit getGrain() {
        return grain;
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Returns {@code true} if this tile is boundary-aligned, meaning its start and end both fall on natural grain
     * boundaries, and it spans exactly one full grain unit.
     *
     * <p>Partial tiles (head or tail fragments) return {@code false}.
     *
     * @return {@code true} if this tile is boundary-aligned; {@code false} if it is a partial tile
     */
    public boolean isAligned() {
        return aligned;
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * The inclusive start of this tile's range.
     */
    private final T start;

    /**
     * The exclusive end of this tile's range.
     */
    private final T end;

    /**
     * The grain unit of this tile.
     */
    private final ChronoUnit grain;

    /**
     * Whether this tile is boundary-aligned.
     */
    private final boolean aligned;
}
