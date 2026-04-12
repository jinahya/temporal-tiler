package com.github.jinahya.time.temporal.tile;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static com.github.jinahya.time.temporal.tile.TemporalTileAssert.assertTile;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class TemporalTiler_Millennia_Test {

    @Test
    void _PartialHeadAndTail() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalDate.of(2025, 6, 15);
        final var end = LocalDate.of(4500, 3, 10);
        final var grain = ChronoUnit.MILLENNIA;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).hasSize(3);
        assertTile(tiles.getFirst())
                .hasStart(LocalDate.of(2025, 6, 15))
                .hasEnd(LocalDate.of(3000, 1, 1))
                .isNotAligned();
        assertTile(tiles.get(1))
                .hasStart(LocalDate.of(3000, 1, 1))
                .hasEnd(LocalDate.of(4000, 1, 1))
                .isAligned();
        assertTile(tiles.getLast())
                .hasStart(LocalDate.of(4000, 1, 1))
                .hasEnd(LocalDate.of(4500, 3, 10))
                .isNotAligned();
    }

    @Test
    void _AllAligned() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalDate.of(2000, 1, 1);
        final var end = LocalDate.of(4000, 1, 1);
        final var grain = ChronoUnit.MILLENNIA;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).hasSize(2);
        assertThat(tiles).allSatisfy(t -> assertTile(t).isAligned());
    }

    @Test
    void _SmallerThanGrain() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalDate.of(2025, 3, 15);
        final var end = LocalDate.of(2025, 9, 20);
        final var grain = ChronoUnit.MILLENNIA;
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
        final var grain = ChronoUnit.MILLENNIA;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        assertThat(tiles).isEmpty();
    }
}
