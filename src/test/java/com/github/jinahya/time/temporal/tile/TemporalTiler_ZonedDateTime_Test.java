package com.github.jinahya.time.temporal.tile;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static com.github.jinahya.time.temporal.tile.TemporalTileAssert.assertTile;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TemporalTiler#tile(java.time.temporal.Temporal, java.time.temporal.Temporal, ChronoUnit)} with
 * various grains ({@link ChronoUnit#HOURS}, {@link ChronoUnit#DAYS}, {@link ChronoUnit#MONTHS}) using
 * {@link ZonedDateTime}.
 *
 * @see TemporalTiler
 */
@Slf4j
class TemporalTiler_ZonedDateTime_Test {

    @Test
    void _Hours_PartialHeadAndTail() {
        // ------------------------------------------------------------------------------------------------------- given
        final var zone = ZoneId.of("America/New_York");
        final var start = ZonedDateTime.of(2025, 3, 15, 14, 30, 0, 0, zone);
        final var end = ZonedDateTime.of(2025, 3, 15, 17, 45, 0, 0, zone);
        final var grain = ChronoUnit.HOURS;
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
    void _Days_PartialHead() {
        // ------------------------------------------------------------------------------------------------------- given
        final var zone = ZoneId.of("Europe/London");
        final var start = ZonedDateTime.of(2025, 3, 15, 10, 0, 0, 0, zone);
        final var end = ZonedDateTime.of(2025, 3, 18, 0, 0, 0, 0, zone);
        final var grain = ChronoUnit.DAYS;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).hasSize(3);
        assertTile(tiles.getFirst()).isNotAligned();
        assertTile(tiles.get(1)).isAligned();
        assertTile(tiles.get(2)).isAligned();
    }

    @Test
    void _Months_AllAligned() {
        // ------------------------------------------------------------------------------------------------------- given
        final var zone = ZoneId.of("Asia/Tokyo");
        final var start = ZonedDateTime.of(2025, 4, 1, 0, 0, 0, 0, zone);
        final var end = ZonedDateTime.of(2025, 7, 1, 0, 0, 0, 0, zone);
        final var grain = ChronoUnit.MONTHS;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).hasSize(3);
        assertThat(tiles).allSatisfy(t -> assertTile(t).isAligned());
    }

    @Test
    void _EmptyRange() {
        // ------------------------------------------------------------------------------------------------------- given
        final var zone = ZoneId.of("UTC");
        final var start = ZonedDateTime.of(2025, 3, 15, 12, 0, 0, 0, zone);
        final var end = start;
        final var grain = ChronoUnit.HOURS;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        assertThat(tiles).isEmpty();
    }
}
