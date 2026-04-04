package com.github.jinahya.time.temporal.tile;

import java.io.Serial;
import java.io.Serializable;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;

/**
 * A {@link TemporalTile} whose grain is a {@link ChronoUnit}.
 *
 * <p>This is a convenience type alias for {@code TemporalTile<T, ChronoUnit>}, reducing verbosity at usage sites.
 *
 * @param <T> the temporal type (e.g., {@link java.time.LocalDate}, {@link java.time.LocalDateTime},
 *            {@link java.time.Instant})
 * @see ChronoTiler
 */
public final class ChronoTile<
        T extends Temporal & Comparable<? super T> & Serializable
        >
        extends TemporalTile<T, ChronoUnit> {

    @Serial
    private static final long serialVersionUID = -1620309024543757042L;

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new chrono tile.
     *
     * @param start   the inclusive start of the range
     * @param end     the exclusive end of the range
     * @param grain   the {@link ChronoUnit} grain
     * @param aligned whether this tile is boundary-aligned
     */
    ChronoTile(final T start, final T end, final ChronoUnit grain, final boolean aligned) {
        super(start, end, grain, aligned);
    }
}
