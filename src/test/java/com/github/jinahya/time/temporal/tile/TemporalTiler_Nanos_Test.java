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
 * {@link ChronoUnit#NANOS} grain using {@link LocalTime}.
 *
 * @see TemporalTiler
 */
@Slf4j
class TemporalTiler_Nanos_Test {

    @Test
    void _AllAligned() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalTime.of(10, 15, 30, 500_000_000);
        final var end = LocalTime.of(10, 15, 30, 500_000_003);
        final var grain = ChronoUnit.NANOS;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).hasSize(3);
        assertTiles(tiles).isAllAligned();
    }

    @Test
    void _SingleNano() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalTime.of(10, 15, 30, 500_000_000);
        final var end = LocalTime.of(10, 15, 30, 500_000_001);
        final var grain = ChronoUnit.NANOS;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).hasSize(1);
        assertTile(tiles.getFirst()).isAligned();
    }

    @Test
    void _EmptyRange() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalTime.of(10, 15, 30, 500_000_000);
        final var end = start;
        final var grain = ChronoUnit.NANOS;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        assertThat(tiles).isEmpty();
    }
}
