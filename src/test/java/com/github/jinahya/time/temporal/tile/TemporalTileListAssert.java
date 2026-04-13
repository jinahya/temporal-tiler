package com.github.jinahya.time.temporal.tile;

import org.assertj.core.api.AbstractIterableAssert;

import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.util.List;

import static com.github.jinahya.time.temporal.tile.TemporalTileAssert.assertTile;
import static org.assertj.core.api.Assertions.assertThat;

class TemporalTileListAssert<T extends Temporal & Comparable<? super T>>
        extends AbstractIterableAssert<
        TemporalTileListAssert<T>,
        List<? extends TemporalTile<T>>,
        TemporalTile<T>,
        TemporalTileAssert<T>> {

    static <T extends Temporal & Comparable<? super T>>
    TemporalTileListAssert<T> assertTiles(final List<TemporalTile<T>> actual) {
        return new TemporalTileListAssert<>(actual);
    }

    // -----------------------------------------------------------------------------------------------------------------
    TemporalTileListAssert(final List<TemporalTile<T>> actual) {
        super(actual, TemporalTileListAssert.class);
    }

    // -----------------------------------------------------------------------------------------------------------------
    @Override
    protected TemporalTileAssert<T> toAssert(final TemporalTile<T> value, final String description) {
        return new TemporalTileAssert<>(value).as(description);
    }

    @Override
    protected TemporalTileListAssert<T> newAbstractIterableAssert(Iterable<? extends TemporalTile<T>> iterable) {
        @SuppressWarnings("unchecked")
        final var list = (List<TemporalTile<T>>) iterable;
        return new TemporalTileListAssert<>(list);
    }

    // ----------------------------------------------------------------------------------------------------------- grain

    /**
     * Asserts that all tiles have the specified grain.
     */
    TemporalTileListAssert<T> hasGrain(final TemporalUnit expectedGrain) {
        isNotNull();
        final var tiles = (List<? extends TemporalTile<T>>) actual;
        for (final var tile : tiles) {
            assertTile(tile).hasGrain(expectedGrain);
        }
        return myself;
    }

    // ------------------------------------------------------------------------------------------------------ contiguous

    /**
     * Asserts that tiles are contiguous: the end of each tile equals the start of the next.
     */
    TemporalTileListAssert<T> isContiguous() {
        isNotNull();
        final var tiles = (List<? extends TemporalTile<T>>) actual;
        for (var i = 1; i < tiles.size(); i++) {
            assertTile(tiles.get(i))
                    .as("tile[%d] should start where tile[%d] ends", i, i - 1)
                    .hasStart(tiles.get(i - 1).end());
        }
        return myself;
    }

    // ------------------------------------------------------------------------------------------------ interior aligned
    /**
     * Asserts that all interior tiles (excluding first and last) are aligned.
     */
    TemporalTileListAssert<T> hasAlignedInterior() {
        isNotNull();
        final var tiles = (List<? extends TemporalTile<T>>) actual;
        for (var i = 1; i < tiles.size() - 1; i++) {
            assertTile(tiles.get(i))
                    .as("interior tile at index %d should be aligned: %s", i, tiles.get(i))
                    .isAligned();
        }
        return myself;
    }

    // ----------------------------------------------------------------------------------------------------------- range

    /**
     * Asserts that the first tile starts at the expected value.
     */
    TemporalTileListAssert<T> startsAt(final T expectedStart) {
        isNotNull();
        final var tiles = (List<? extends TemporalTile<T>>) actual;
        assertThat(tiles).isNotEmpty();
        assertTile(tiles.getFirst()).hasStart(expectedStart);
        return myself;
    }

    /**
     * Asserts that the last tile ends at the expected value.
     */
    TemporalTileListAssert<T> endsAt(final T expectedEnd) {
        isNotNull();
        final var tiles = (List<? extends TemporalTile<T>>) actual;
        assertThat(tiles).isNotEmpty();
        assertTile(tiles.getLast()).hasEnd(expectedEnd);
        return myself;
    }

    // ----------------------------------------------------------------------------------------------------- all aligned

    /**
     * Asserts that all tiles are aligned.
     */
    TemporalTileListAssert<T> isAllAligned() {
        isNotNull();
        final var tiles = (List<? extends TemporalTile<T>>) actual;
        for (final var tile : tiles) {
            assertTile(tile).isAligned();
        }
        return myself;
    }

    /**
     * Asserts that no tile is aligned (all partial).
     */
    TemporalTileListAssert<T> isNoneAligned() {
        isNotNull();
        final var tiles = (List<? extends TemporalTile<T>>) actual;
        for (final var tile : tiles) {
            assertTile(tile).isNotAligned();
        }
        return myself;
    }
}
