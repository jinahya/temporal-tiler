package com.github.jinahya.time.temporal.tile;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static com.github.jinahya.time.temporal.tile.TemporalTileAssert.assertTile;
import static com.github.jinahya.time.temporal.tile.TemporalTileListAssert.assertTiles;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TemporalTiler#tile(java.time.temporal.Temporal, java.time.temporal.Temporal, ChronoUnit)}, which
 * decomposes a half-open temporal range {@code [start, end)} into a list of non-overlapping, gap-free
 * {@link TemporalTile}s at a single {@link ChronoUnit} grain.
 *
 * @see TemporalTiler
 * @see TemporalTileAssert
 */
@Slf4j
class TemporalTiler_LocalDate_Test {

    @Test
    void _Months_PartialHeadAndTail() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalDate.of(2025, 3, 15);
        final var end = LocalDate.of(2025, 6, 10);
        final var grain = ChronoUnit.MONTHS;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).hasSize(4);
        assertTile(tiles.getFirst())
                .hasStart(LocalDate.of(2025, 3, 15))
                .hasEnd(LocalDate.of(2025, 4, 1))
                .isNotAligned();
        assertTile(tiles.get(1)).isAligned();
        assertTile(tiles.get(2)).isAligned();
        assertTile(tiles.getLast())
                .hasStart(LocalDate.of(2025, 6, 1))
                .hasEnd(LocalDate.of(2025, 6, 10))
                .isNotAligned();
    }

    @Test
    void _Days_AllAligned() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalDate.of(2025, 3, 15);
        final var end = LocalDate.of(2025, 3, 18);
        final var grain = ChronoUnit.DAYS;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).hasSize(3);
        assertTiles(tiles).isAllAligned();
    }

    @Test
    void _Years_PartialHeadAndTail() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalDate.of(2025, 3, 15);
        final var end = LocalDate.of(2027, 6, 10);
        final var grain = ChronoUnit.YEARS;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).hasSize(3);
        assertTile(tiles.getFirst()).isNotAligned();
        assertTile(tiles.get(1)).isAligned();
        assertTile(tiles.getLast()).isNotAligned();
    }

    @Test
    void _Weeks_PartialHeadAndTail() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalDate.of(2025, 3, 12);
        final var end = LocalDate.of(2025, 3, 27);
        final var grain = ChronoUnit.WEEKS;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).hasSize(3);
        assertTile(tiles.getFirst())
                .hasStart(LocalDate.of(2025, 3, 12))
                .hasEnd(LocalDate.of(2025, 3, 17))
                .isNotAligned();
        assertTile(tiles.get(1)).isAligned();
        assertTile(tiles.getLast()).isNotAligned();
    }

    @Test
    void _EmptyRange() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalDate.of(2025, 3, 15);
        final var end = start;
        final var grain = ChronoUnit.MONTHS;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        assertThat(tiles).isEmpty();
    }

    @Test
    void _SmallerThanGrain() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalDate.of(2025, 3, 15);
        final var end = LocalDate.of(2025, 3, 20);
        final var grain = ChronoUnit.MONTHS;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).hasSize(1);
        assertTile(tiles.getFirst()).isNotAligned();
    }

    @Test
    void _Months_AlignedStartAndEnd() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalDate.of(2025, 4, 1);
        final var end = LocalDate.of(2025, 7, 1);
        final var grain = ChronoUnit.MONTHS;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).hasSize(3);
        assertTiles(tiles).isAllAligned();
    }

    @Test
    void _GapFree() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalDate.of(2025, 3, 15);
        final var end = LocalDate.of(2025, 8, 22);
        final var grain = ChronoUnit.MONTHS;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertTiles(tiles).startsAt(start).endsAt(end).isContiguous();
    }

    @Test
    void _Hierarchical_UserDriven() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalDate.of(2025, 3, 15);
        final var end = LocalDate.of(2027, 6, 10);
        // -------------------------------------------------------------------------------------------------------- when
        final var yearTiles = TemporalTiler.tile(start, end, ChronoUnit.YEARS);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(ChronoUnit.YEARS, yearTiles);
        assertThat(yearTiles).hasSize(3);
        final var headMonths = TemporalTiler.tile(
                yearTiles.getFirst().start(),
                yearTiles.getFirst().end(),
                ChronoUnit.MONTHS);
        TemporalTiles_TestUtils.verify(ChronoUnit.MONTHS, headMonths);
        assertThat(headMonths).hasSize(10);
        assertTile(headMonths.getFirst()).isNotAligned();
        assertTile(headMonths.get(1)).isAligned();
    }
}
