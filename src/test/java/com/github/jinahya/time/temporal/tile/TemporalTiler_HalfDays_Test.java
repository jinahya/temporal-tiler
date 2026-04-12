package com.github.jinahya.time.temporal.tile;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import static com.github.jinahya.time.temporal.tile.TemporalTileAssert.assertTile;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class TemporalTiler_HalfDays_Test {

    @Test
    void _LocalTime_PartialHeadOnly() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalTime.of(10, 30);
        final var end = LocalTime.of(12, 0);
        final var grain = ChronoUnit.HALF_DAYS;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).hasSize(1);
        assertTile(tiles.getFirst())
                .hasStart(LocalTime.of(10, 30))
                .hasEnd(LocalTime.of(12, 0))
                .isNotAligned();
    }

    @Test
    void _LocalTime_AllAligned() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalTime.of(0, 0);
        final var end = LocalTime.of(12, 0);
        final var grain = ChronoUnit.HALF_DAYS;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).hasSize(1);
        assertTile(tiles.getFirst()).isAligned();
    }

    @Test
    void _LocalDateTime_PartialHeadAndTail() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalDateTime.of(2025, 3, 15, 10, 30);
        final var end = LocalDateTime.of(2025, 3, 16, 8, 15);
        final var grain = ChronoUnit.HALF_DAYS;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).hasSize(3);
        assertTile(tiles.getFirst())
                .hasStart(LocalDateTime.of(2025, 3, 15, 10, 30))
                .hasEnd(LocalDateTime.of(2025, 3, 15, 12, 0))
                .isNotAligned();
        assertTile(tiles.get(1))
                .hasStart(LocalDateTime.of(2025, 3, 15, 12, 0))
                .hasEnd(LocalDateTime.of(2025, 3, 16, 0, 0))
                .isAligned();
        assertTile(tiles.getLast())
                .hasStart(LocalDateTime.of(2025, 3, 16, 0, 0))
                .hasEnd(LocalDateTime.of(2025, 3, 16, 8, 15))
                .isNotAligned();
    }

    @Test
    void _EmptyRange() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalTime.of(10, 30);
        final var end = start;
        final var grain = ChronoUnit.HALF_DAYS;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        assertThat(tiles).isEmpty();
    }
}
