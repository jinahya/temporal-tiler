package com.github.jinahya.time.temporal.tile;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static com.github.jinahya.time.temporal.tile.TemporalTileAssert.assertTile;
import static com.github.jinahya.time.temporal.tile.TemporalTileListAssert.assertTiles;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TemporalTiler#tile(java.time.temporal.Temporal, java.time.temporal.Temporal, ChronoUnit)} with
 * {@link ChronoUnit#WEEKS} and {@link ChronoUnit#YEARS} grains using {@link LocalDate}, including hierarchical
 * year-then-week tiling.
 *
 * @see TemporalTiler
 */
@Slf4j
class TemporalTiler_Years_Weeks_Test {

    @Test
    void _SingleAlignedYear_2025() {
        // ------------------------------------------------------------------------------------------------------- given
        // 2025-01-01 is a Wednesday; week boundary (Monday) is 2025-01-06
        final var start = LocalDate.of(2025, 1, 1);
        final var end = LocalDate.of(2026, 1, 1);
        final var grain = ChronoUnit.WEEKS;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(grain, tiles);
        // head partial: [Jan 1 Wed, Jan 6 Mon) + 51 aligned weeks + tail partial: [Dec 29 Mon, Jan 1 Thu)
        assertThat(tiles).hasSize(53);
        assertTile(tiles.getFirst())
                .hasStart(LocalDate.of(2025, 1, 1))
                .hasEnd(LocalDate.of(2025, 1, 6))
                .isNotAligned();
        assertTiles(tiles.subList(1, 52)).isAllAligned();
        assertTile(tiles.getLast())
                .hasStart(LocalDate.of(2025, 12, 29))
                .hasEnd(LocalDate.of(2026, 1, 1))
                .isNotAligned();
    }

    @Test
    void _SingleAlignedYear_StartsOnMonday() {
        // ------------------------------------------------------------------------------------------------------- given
        // 2024-01-01 is a Monday — week-aligned start
        final var start = LocalDate.of(2024, 1, 1);
        final var end = LocalDate.of(2025, 1, 1);
        final var grain = ChronoUnit.WEEKS;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        // 2024 is a leap year: 366 days = 52 weeks + 2 days
        // no head partial (starts on Monday); 52 aligned weeks + tail partial: [Dec 30 Mon, Jan 1 Wed)
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).hasSize(53);
        assertTile(tiles.getFirst())
                .hasStart(LocalDate.of(2024, 1, 1))
                .isAligned();
        assertTiles(tiles.subList(0, 52)).isAllAligned();
        assertTile(tiles.getLast())
                .hasStart(LocalDate.of(2024, 12, 30))
                .hasEnd(LocalDate.of(2025, 1, 1))
                .isNotAligned();
    }

    @Test
    void _HierarchicalYearsThenWeeks() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalDate.of(2025, 3, 15);
        final var end = LocalDate.of(2027, 6, 10);
        // -------------------------------------------------------------------------------------------------------- when
        final var yearTiles = TemporalTiler.tile(start, end, ChronoUnit.YEARS);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(ChronoUnit.YEARS, yearTiles);
        assertThat(yearTiles).hasSize(3);
        assertTile(yearTiles.getFirst()).isNotAligned();
        assertTile(yearTiles.get(1)).isAligned();
        assertTile(yearTiles.getLast()).isNotAligned();
        // tile each year-tile into weeks
        for (var yearTile : yearTiles) {
            final var weekTiles = TemporalTiler.tile(yearTile, ChronoUnit.WEEKS);
            TemporalTiles_TestUtils.verify(ChronoUnit.WEEKS, weekTiles);
            log.debug("year tile {} -> {} week tiles", yearTile, weekTiles.size());
            // gap-free: first week tile starts at year tile start, last ends at year tile end
            assertTiles(weekTiles).isNotEmpty();
            assertTiles(weekTiles).startsAt(yearTile.start()).endsAt(yearTile.end());
        }
    }

    @Test
    void _PartialYearRange_IntoWeeks() {
        // ------------------------------------------------------------------------------------------------------- given
        // a partial year: [2025-03-15, 2026-01-01) — the head tile from tiling by YEARS
        final var start = LocalDate.of(2025, 3, 15); // Saturday
        final var end = LocalDate.of(2026, 1, 1);    // Thursday
        final var grain = ChronoUnit.WEEKS;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(grain, tiles);
        // head partial: [Mar 15 Sat, Mar 17 Mon)
        assertTile(tiles.getFirst())
                .hasStart(LocalDate.of(2025, 3, 15))
                .hasEnd(LocalDate.of(2025, 3, 17))
                .isNotAligned();
        // tail partial: [Dec 29 Mon, Jan 1 Thu)
        assertTile(tiles.getLast())
                .hasStart(LocalDate.of(2025, 12, 29))
                .hasEnd(LocalDate.of(2026, 1, 1))
                .isNotAligned();
        // middle tiles are all aligned weeks
        assertTiles(tiles.subList(1, tiles.size() - 1)).isAllAligned();
    }
}
