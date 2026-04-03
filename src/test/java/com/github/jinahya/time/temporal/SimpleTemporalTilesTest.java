package com.github.jinahya.time.temporal;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
class SimpleTemporalTilesTest {

    @DisplayName("2026-04-06, 2026-04-13, DAYS")
    @Test
    void tile_Days_ExactWeek() {
        // ------------------------------------------------------------------------------------------------------- given
        final var start = LocalDate.of(2026, 4, 6);  // Monday
        final var end = LocalDate.of(2026, 4, 13);    // next Monday
        // -------------------------------------------------------------------------------------------------------- when
        final var tiles = SimpleTemporalTiles.tile(start, end, ChronoUnit.DAYS);
        tiles.forEach(t -> {
            log.debug("tile: {}", t);
        });
        // -------------------------------------------------------------------------------------------------------- then
        assertEquals(7, tiles.size());
        for (var tile : tiles) {
            assertEquals(ChronoUnit.DAYS, tile.getUnit());
        }
        assertEquals(start, tiles.getFirst().getStartInclusive());
        assertEquals(end, tiles.getLast().getEndExclusive());
    }

    @Test
    void tile_Months_FullYear() {
        var start = LocalDate.of(2026, 1, 1);
        var end = LocalDate.of(2027, 1, 1);
        var tiles = SimpleTemporalTiles.tile(start, end, ChronoUnit.MONTHS);
        assertEquals(12, tiles.size());
        assertEquals(LocalDate.of(2026, 1, 1), tiles.getFirst().getStartInclusive());
        assertEquals(LocalDate.of(2026, 2, 1), tiles.getFirst().getEndExclusive());
        assertEquals(LocalDate.of(2026, 12, 1), tiles.getLast().getStartInclusive());
        assertEquals(LocalDate.of(2027, 1, 1), tiles.getLast().getEndExclusive());
    }

    @Test
    void tile_Months_PartialRange() {
        var start = LocalDate.of(2026, 3, 15);
        var end = LocalDate.of(2026, 6, 10);
        var tiles = SimpleTemporalTiles.tile(start, end, ChronoUnit.MONTHS);
        assertEquals(3, tiles.size());
        // first tile: partial month
        assertEquals(start, tiles.getFirst().getStartInclusive());
        assertEquals(LocalDate.of(2026, 4, 15), tiles.getFirst().getEndExclusive());
        // last tile: shorter than a full month
        assertEquals(end, tiles.getLast().getEndExclusive());
    }

    @Test
    void tile_Years() {
        var start = LocalDate.of(2020, 1, 1);
        var end = LocalDate.of(2025, 1, 1);
        var tiles = SimpleTemporalTiles.tile(start, end, ChronoUnit.YEARS);
        assertEquals(5, tiles.size());
        for (var tile : tiles) {
            assertEquals(ChronoUnit.YEARS, tile.getUnit());
        }
    }

    @Test
    void tile_Hours_LocalDateTime() {
        var start = LocalDateTime.of(2026, 4, 3, 10, 0);
        var end = LocalDateTime.of(2026, 4, 3, 15, 0);
        var tiles = SimpleTemporalTiles.tile(start, end, ChronoUnit.HOURS);
        assertEquals(5, tiles.size());
        assertEquals(start, tiles.getFirst().getStartInclusive());
        assertEquals(end, tiles.getLast().getEndExclusive());
    }

    @Test
    void tile_SingleTile_RangeSmallerThanUnit() {
        var start = LocalDate.of(2026, 4, 3);
        var end = LocalDate.of(2026, 4, 10);
        var tiles = SimpleTemporalTiles.tile(start, end, ChronoUnit.MONTHS);
        assertEquals(1, tiles.size());
        assertEquals(start, tiles.getFirst().getStartInclusive());
        assertEquals(end, tiles.getFirst().getEndExclusive());
    }

    @Test
    void tile_NoOverlap_NoGap() {
        var start = LocalDate.of(2025, 3, 15);
        var end = LocalDate.of(2027, 6, 10);
        var tiles = SimpleTemporalTiles.tile(start, end, ChronoUnit.MONTHS);
        // verify contiguity: each tile's end == next tile's start
        for (int i = 0; i < tiles.size() - 1; i++) {
            assertEquals(tiles.get(i).getEndExclusive(), tiles.get(i + 1).getStartInclusive());
        }
        assertEquals(start, tiles.getFirst().getStartInclusive());
        assertEquals(end, tiles.getLast().getEndExclusive());
    }

    @Test
    void tile_ResultIsUnmodifiable() {
        var tiles = SimpleTemporalTiles.tile(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10), ChronoUnit.DAYS);
        assertThrows(UnsupportedOperationException.class, () -> tiles.add(null));
    }

    @Test
    void tile_EmptyRange_Throws() {
        var date = LocalDate.of(2026, 4, 3);
        assertThrows(IllegalArgumentException.class,
                     () -> SimpleTemporalTiles.tile(date, date, ChronoUnit.DAYS));
    }

    @Test
    void tile_ReversedRange_Throws() {
        assertThrows(IllegalArgumentException.class,
                     () -> SimpleTemporalTiles.tile(
                             LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 3), ChronoUnit.DAYS));
    }

    @Test
    void tile_NullArgs_Throws() {
        assertThrows(NullPointerException.class,
                     () -> SimpleTemporalTiles.tile(null, LocalDate.of(2026, 4, 3), ChronoUnit.DAYS));
        assertThrows(NullPointerException.class,
                     () -> SimpleTemporalTiles.tile(LocalDate.of(2026, 4, 3), null, ChronoUnit.DAYS));
        assertThrows(NullPointerException.class,
                     () -> SimpleTemporalTiles.tile(LocalDate.of(2026, 4, 3), LocalDate.of(2026, 4, 10), null));
    }
}
