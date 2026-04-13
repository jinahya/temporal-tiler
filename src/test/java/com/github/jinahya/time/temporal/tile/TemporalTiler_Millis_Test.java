package com.github.jinahya.time.temporal.tile;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import static com.github.jinahya.time.temporal.tile.TemporalTileAssert.assertTile;
import static com.github.jinahya.time.temporal.tile.TemporalTileListAssert.assertTiles;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TemporalTiler#tile(java.time.temporal.Temporal, java.time.temporal.Temporal, ChronoUnit)} with
 * {@link ChronoUnit#MILLIS} grain using {@link LocalTime}.
 *
 * @see TemporalTiler
 */
@Slf4j
class TemporalTiler_Millis_Test {

    @Test
    void _PartialHeadAndTail() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalTime.of(10, 15, 30, 500_500_000);
        final var end = LocalTime.of(10, 15, 30, 503_200_000);
        final var grain = ChronoUnit.MILLIS;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).hasSize(4);
        assertTile(tiles.getFirst())
                .hasStart(LocalTime.of(10, 15, 30, 500_500_000))
                .hasEnd(LocalTime.of(10, 15, 30, 501_000_000))
                .isNotAligned();
        assertTile(tiles.get(1)).isAligned();
        assertTile(tiles.get(2)).isAligned();
        assertTile(tiles.getLast())
                .hasStart(LocalTime.of(10, 15, 30, 503_000_000))
                .hasEnd(LocalTime.of(10, 15, 30, 503_200_000))
                .isNotAligned();
    }

    @Test
    void _AllAligned() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalTime.of(10, 15, 30, 500_000_000);
        final var end = LocalTime.of(10, 15, 30, 503_000_000);
        final var grain = ChronoUnit.MILLIS;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).hasSize(3);
        assertTiles(tiles).isAllAligned();
    }

    @Test
    void _EmptyRange() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalTime.of(10, 15, 30, 500_000_000);
        final var end = start;
        final var grain = ChronoUnit.MILLIS;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        assertThat(tiles).isEmpty();
    }
}
