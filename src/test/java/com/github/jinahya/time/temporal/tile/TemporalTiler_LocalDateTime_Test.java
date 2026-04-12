package com.github.jinahya.time.temporal.tile;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static com.github.jinahya.time.temporal.tile.TemporalTileAssert.assertTile;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class TemporalTiler_LocalDateTime_Test {

    @Test
    void _Hours_PartialHeadAndTail() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalDateTime.of(2025, 3, 15, 14, 30);
        final var end = LocalDateTime.of(2025, 3, 15, 17, 30);
        final var grain = ChronoUnit.HOURS;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).hasSize(4);
        assertTile(tiles.getFirst())
                .hasStart(LocalDateTime.of(2025, 3, 15, 14, 30))
                .hasEnd(LocalDateTime.of(2025, 3, 15, 15, 0))
                .isNotAligned();
        assertTile(tiles.get(1)).isAligned();
        assertTile(tiles.get(2)).isAligned();
        assertTile(tiles.getLast())
                .hasStart(LocalDateTime.of(2025, 3, 15, 17, 0))
                .hasEnd(LocalDateTime.of(2025, 3, 15, 17, 30))
                .isNotAligned();
    }

    @Test
    void _Hours_PartialHead() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalDateTime.of(2025, 3, 15, 14, 30);
        final var end = LocalDateTime.of(2025, 3, 15, 17, 0);
        final var grain = ChronoUnit.HOURS;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).hasSize(3);
        assertTile(tiles.getFirst())
                .hasStart(LocalDateTime.of(2025, 3, 15, 14, 30))
                .hasEnd(LocalDateTime.of(2025, 3, 15, 15, 0))
                .isNotAligned();
        assertTile(tiles.get(1)).isAligned();
        assertTile(tiles.get(2)).isAligned();
    }

    @Test
    void _Days_PartialHeadAndTail() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalDateTime.of(2025, 3, 15, 10, 0);
        final var end = LocalDateTime.of(2025, 3, 18, 14, 0);
        final var grain = ChronoUnit.DAYS;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).hasSize(4);
        assertTile(tiles.getFirst())
                .hasStart(LocalDateTime.of(2025, 3, 15, 10, 0))
                .hasEnd(LocalDateTime.of(2025, 3, 16, 0, 0))
                .isNotAligned();
        assertTile(tiles.get(1)).isAligned();
        assertTile(tiles.get(2)).isAligned();
        assertTile(tiles.getLast()).isNotAligned();
    }

    @Test
    void _Months_AllAligned() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalDateTime.of(2025, 4, 1, 0, 0);
        final var end = LocalDateTime.of(2025, 7, 1, 0, 0);
        final var grain = ChronoUnit.MONTHS;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).hasSize(3);
        assertThat(tiles).allSatisfy(t -> assertTile(t).isAligned());
    }

    @Test
    void _Minutes_PartialHead() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalDateTime.of(2025, 3, 15, 14, 15, 30);
        final var end = LocalDateTime.of(2025, 3, 15, 14, 18, 0);
        final var grain = ChronoUnit.MINUTES;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).hasSize(3);
        assertTile(tiles.getFirst()).isNotAligned();
        assertTile(tiles.get(1)).isAligned();
        assertTile(tiles.get(2)).isAligned();
    }
}
