package com.github.jinahya.time.temporal.tile;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static com.github.jinahya.time.temporal.tile.TemporalTileAssert.assertTile;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class TemporalTiler_OffsetDateTime_Test {

    @Test
    void _Hours_PartialHeadAndTail() {
        // ------------------------------------------------------------------------------------------------------- given
        final var offset = ZoneOffset.ofHours(9);
        final var start = OffsetDateTime.of(2025, 3, 15, 14, 30, 0, 0, offset);
        final var end = OffsetDateTime.of(2025, 3, 15, 17, 15, 0, 0, offset);
        final var grain = ChronoUnit.HOURS;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).hasSize(4);
        assertTile(tiles.getFirst())
                .hasStart(OffsetDateTime.of(2025, 3, 15, 14, 30, 0, 0, offset))
                .hasEnd(OffsetDateTime.of(2025, 3, 15, 15, 0, 0, 0, offset))
                .isNotAligned();
        assertTile(tiles.get(1)).isAligned();
        assertTile(tiles.get(2)).isAligned();
        assertTile(tiles.getLast()).isNotAligned();
    }

    @Test
    void _Days_AllAligned() {
        // ------------------------------------------------------------------------------------------------------- given
        final var offset = ZoneOffset.UTC;
        final var start = OffsetDateTime.of(2025, 3, 15, 0, 0, 0, 0, offset);
        final var end = OffsetDateTime.of(2025, 3, 18, 0, 0, 0, 0, offset);
        final var grain = ChronoUnit.DAYS;
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = TemporalTiler.tile(start, end, grain);
        // -------------------------------------------------------------------------------------------------------- then
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).hasSize(3);
        assertThat(tiles).allSatisfy(t -> assertTile(t).isAligned());
    }

    @Test
    void _Months_PartialHeadAndTail() {
        // ------------------------------------------------------------------------------------------------------- given
        final var offset = ZoneOffset.ofHours(-5);
        final var start = OffsetDateTime.of(2025, 3, 15, 0, 0, 0, 0, offset);
        final var end = OffsetDateTime.of(2025, 6, 10, 0, 0, 0, 0, offset);
        final var grain = ChronoUnit.MONTHS;
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
}
