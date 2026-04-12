package com.github.jinahya.time.temporal.tile;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

import static com.github.jinahya.time.temporal.tile.TemporalTileAssert.assertTile;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class TemporalTiler_LocalTime_Test {

    @Test
    void _MINUTES_() {
        // ------------------------------------------------------------------------------------------------------- given
        final var grain = ChronoUnit.MINUTES;
        final var start = LocalTime.now();
        final var end = start.plusNanos(
                ThreadLocalRandom.current().nextLong()
                & 3_600_000_000_000L
                | 600_000_000_000L
        );
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        tiles.forEach(t -> {
            log.debug("tile: {}", t);
        });
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).isNotEmpty();
        assertTile(tiles.getFirst()).hasStart(start);
        assertTile(tiles.getLast()).hasEnd(end);
    }

    @Test
    void _HOURS_() {
        // ------------------------------------------------------------------------------------------------------- given
        final var grain = ChronoUnit.HOURS;
        final var start = LocalTime.now();
        final var end =
                start.plusNanos(
                        ThreadLocalRandom.current().nextLong()
                        & 86_400_000_000_000L
                        | 10_800_000_000_000L
                );
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        tiles.forEach(t -> {
            log.debug("tile: {}", t);
        });
    }

    @DisplayName("[14:30, 17:45) by HOURS")
    @Test
    void _Hours_PartialHeadAndTail() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalTime.of(14, 30);
        final var end = LocalTime.of(17, 45);
        final var grain = ChronoUnit.HOURS;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).hasSize(4);
        assertTile(tiles.getFirst())
                .hasStart(start)
                .hasEnd(LocalTime.of(15, 0))
                .isNotAligned();
        assertTile(tiles.get(1))
                .hasStart(LocalTime.of(15, 0))
                .hasEnd(LocalTime.of(16, 0))
                .isAligned();
        assertTile(tiles.get(2))
                .hasStart(LocalTime.of(16, 0))
                .hasEnd(LocalTime.of(17, 0))
                .isAligned();
        assertTile(tiles.getLast())
                .hasStart(LocalTime.of(17, 0))
                .hasEnd(end)
                .isNotAligned();
    }

    @Test
    void _Hours_AllAligned() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalTime.of(10, 0);
        final var end = LocalTime.of(13, 0);
        final var grain = ChronoUnit.HOURS;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).hasSize(3);
        assertThat(tiles).allSatisfy(t -> assertTile(t).isAligned());
    }

    @Test
    void _Minutes_PartialHeadAndTail() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalTime.of(10, 15, 30);
        final var end = LocalTime.of(10, 18, 20);
        final var grain = ChronoUnit.MINUTES;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).hasSize(4);
        assertTile(tiles.getFirst()).isNotAligned();
        assertTile(tiles.get(1)).isAligned();
        assertTile(tiles.get(2)).isAligned();
        assertTile(tiles.getLast()).isNotAligned();
    }

    @Test
    void _EmptyRange() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalTime.of(12, 0);
        final var end = start;
        final var grain = ChronoUnit.HOURS;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        assertThat(tiles).isEmpty();
    }

    @Test
    void _SmallerThanGrain() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalTime.of(10, 15);
        final var end = LocalTime.of(10, 45);
        final var grain = ChronoUnit.HOURS;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).hasSize(1);
        assertTile(tiles.getFirst())
                .hasStart(LocalTime.of(10, 15))
                .hasEnd(LocalTime.of(10, 45))
                .isNotAligned();
    }
}
