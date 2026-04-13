package com.github.jinahya.time.temporal.tile;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

import static com.github.jinahya.time.temporal.tile.TemporalTileAssert.assertTile;
import static com.github.jinahya.time.temporal.tile.TemporalTileListAssert.assertTiles;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TemporalTiler#tile(java.time.temporal.Temporal, java.time.temporal.Temporal, ChronoUnit)} with
 * {@link ChronoUnit#MILLENNIA} grain using {@link LocalDate}.
 *
 * @see TemporalTiler
 */
@Slf4j
class TemporalTiler_Millennia_Test {

    @Test
    void __() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = YearMonth.of(2025, 6);
        final var end = YearMonth.of(4500, 3);
        final var grain = ChronoUnit.MILLENNIA;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).hasSize(3);
        assertTile(tiles.getFirst())
                .hasStart(start)
                .hasEnd(YearMonth.of(3000, 1))
                .isNotAligned();
        assertTile(tiles.get(1))
                .hasStart(YearMonth.of(3000, 1))
                .hasEnd(YearMonth.of(4000, 1))
                .isAligned();
        assertTile(tiles.getLast())
                .hasStart(YearMonth.of(4000, 1))
                .hasEnd(end)
                .isNotAligned();
    }

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
                .hasStart(start)
                .hasEnd(LocalDate.of(3000, 1, 1))
                .isNotAligned();
        assertTile(tiles.get(1))
                .hasStart(LocalDate.of(3000, 1, 1))
                .hasEnd(LocalDate.of(4000, 1, 1))
                .isAligned();
        assertTile(tiles.getLast())
                .hasStart(LocalDate.of(4000, 1, 1))
                .hasEnd(end)
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
        assertTiles(tiles).isAllAligned();
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
