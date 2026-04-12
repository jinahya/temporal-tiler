package com.github.jinahya.time.temporal.tile;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static com.github.jinahya.time.temporal.tile.TemporalTileAssert.assertTile;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class TemporalTiler_Centuries_Test {

    @Test
    void _PartialHeadAndTail() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalDate.of(2025, 6, 15);
        final var end = LocalDate.of(2350, 3, 10);
        final var grain = ChronoUnit.CENTURIES;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).hasSize(4);
        assertTile(tiles.getFirst())
                .hasStart(LocalDate.of(2025, 6, 15))
                .hasEnd(LocalDate.of(2100, 1, 1))
                .isNotAligned();
        assertTile(tiles.get(1))
                .hasStart(LocalDate.of(2100, 1, 1))
                .hasEnd(LocalDate.of(2200, 1, 1))
                .isAligned();
        assertTile(tiles.get(2))
                .hasStart(LocalDate.of(2200, 1, 1))
                .hasEnd(LocalDate.of(2300, 1, 1))
                .isAligned();
        assertTile(tiles.getLast())
                .hasStart(LocalDate.of(2300, 1, 1))
                .hasEnd(LocalDate.of(2350, 3, 10))
                .isNotAligned();
    }

    @Test
    void _AllAligned() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalDate.of(2000, 1, 1);
        final var end = LocalDate.of(2300, 1, 1);
        final var grain = ChronoUnit.CENTURIES;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).hasSize(3);
        assertThat(tiles).allSatisfy(t -> assertTile(t).isAligned());
    }

    @Test
    void _SmallerThanGrain() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalDate.of(2025, 3, 15);
        final var end = LocalDate.of(2080, 9, 20);
        final var grain = ChronoUnit.CENTURIES;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).hasSize(1);
        assertTile(tiles.getFirst()).isNotAligned();
    }

    @Test
    void _EmptyRange() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalDate.of(2025, 3, 15);
        final var end = start;
        final var grain = ChronoUnit.CENTURIES;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        assertThat(tiles).isEmpty();
    }
}
