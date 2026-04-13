package com.github.jinahya.time.temporal.tile;

import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test utilities for verifying invariants of a list of {@link TemporalTile}s produced by {@link TemporalTiler}.
 *
 * @see TemporalTiler
 * @see TemporalTile
 */
final class TemporalTiles_TestUtils {

    /**
     * Verifies that the given list of tiles satisfies the tiling contract: no nulls, correct grain, non-empty tiles,
     * aligned/partial grain-span consistency, contiguity, and interior alignment.
     *
     * @param <T>   the temporal type
     * @param grain the expected grain for all tiles
     * @param tiles the list of tiles to verify
     */
    static <T extends Temporal & Comparable<? super T>> void verify(final ChronoUnit grain,
                                                                    final List<TemporalTile<T>> tiles) {
        Objects.requireNonNull(grain, "grain is null");
        Objects.requireNonNull(tiles, "tiles is null");
        assertThat(tiles)
                .doesNotContainNull()
                .extracting(TemporalTile::grain)
                .containsOnly(grain);
        for (final var tile : tiles) {
            // each tile must be non-empty: start < end
            assertThat(tile.start().compareTo(tile.end()))
                    .as("tile start (%s) must be before end (%s)", tile.start(), tile.end())
                    .isNegative();
            // aligned tiles span exactly one grain; partial tiles span less
            final var grainSpan = grain.between(tile.start(), tile.end());
            if (tile.aligned()) {
                assertThat(grainSpan)
                        .as("aligned tile [%s, %s) should span exactly 1 %s", tile.start(), tile.end(), grain)
                        .isEqualTo(1L);
            } else {
                assertThat(grainSpan)
                        .as("partial tile [%s, %s) should span less than 1 %s", tile.start(), tile.end(), grain)
                        .isZero();
            }
        }
        // tiles must be contiguous: end of tile i-1 == start of tile i
        for (var i = 1; i < tiles.size(); i++) {
            assertThat(grain.between(tiles.get(i - 1).end(), tiles.get(i).start())).isZero();
        }
        // only the first and last tiles may be partial; all interior tiles must be aligned
        for (var i = 1; i < tiles.size() - 1; i++) {
            assertThat(tiles.get(i).aligned())
                    .as("interior tile at index %d should be aligned: %s", i, tiles.get(i))
                    .isTrue();
        }
    }

    private TemporalTiles_TestUtils() {
        throw new AssertionError("instantiation is not allowed");
    }
}
