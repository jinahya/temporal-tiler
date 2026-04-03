package com.github.jinahya.time.temporal;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemporalTilerTest {

    @Test
    void tile_Months_PartialHeadAndTail() {
        // [Mar 15, Jun 10) by MONTHS
        var tiles = TemporalTiler.<LocalDate>of(ChronoUnit.MONTHS)
                .tile(LocalDate.of(2025, 3, 15), LocalDate.of(2025, 6, 10))
;

        assertEquals(4, tiles.size());

        // Head partial: [Mar 15, Apr 1)
        assertEquals(LocalDate.of(2025, 3, 15), tiles.get(0).getStartInclusive());
        assertEquals(LocalDate.of(2025, 4, 1), tiles.get(0).getEndExclusive());
        assertFalse(tiles.get(0).isAligned());

        // Full month: [Apr 1, May 1)
        assertEquals(LocalDate.of(2025, 4, 1), tiles.get(1).getStartInclusive());
        assertEquals(LocalDate.of(2025, 5, 1), tiles.get(1).getEndExclusive());
        assertTrue(tiles.get(1).isAligned());

        // Full month: [May 1, Jun 1)
        assertEquals(LocalDate.of(2025, 5, 1), tiles.get(2).getStartInclusive());
        assertEquals(LocalDate.of(2025, 6, 1), tiles.get(2).getEndExclusive());
        assertTrue(tiles.get(2).isAligned());

        // Tail partial: [Jun 1, Jun 10)
        assertEquals(LocalDate.of(2025, 6, 1), tiles.get(3).getStartInclusive());
        assertEquals(LocalDate.of(2025, 6, 10), tiles.get(3).getEndExclusive());
        assertFalse(tiles.get(3).isAligned());
    }

    @Test
    void tile_Months_AlignedStartAndEnd() {
        // [Apr 1, Jul 1) by MONTHS — perfectly aligned
        var tiles = TemporalTiler.<LocalDate>of(ChronoUnit.MONTHS)
                .tile(LocalDate.of(2025, 4, 1), LocalDate.of(2025, 7, 1))
;

        assertEquals(3, tiles.size());
        assertTrue(tiles.stream().allMatch(TemporalTile::isAligned));
    }

    @Test
    void tile_Years_PartialHeadAndTail() {
        // [Mar 15, Jun 10 next year) by YEARS
        var tiles = TemporalTiler.<LocalDate>of(ChronoUnit.YEARS)
                .tile(LocalDate.of(2025, 3, 15), LocalDate.of(2027, 6, 10))
;

        assertEquals(3, tiles.size());

        // Head partial: [2025-03-15, 2026-01-01)
        assertFalse(tiles.get(0).isAligned());
        assertEquals(LocalDate.of(2025, 3, 15), tiles.get(0).getStartInclusive());
        assertEquals(LocalDate.of(2026, 1, 1), tiles.get(0).getEndExclusive());

        // Full year: [2026-01-01, 2027-01-01)
        assertTrue(tiles.get(1).isAligned());
        assertEquals(LocalDate.of(2026, 1, 1), tiles.get(1).getStartInclusive());
        assertEquals(LocalDate.of(2027, 1, 1), tiles.get(1).getEndExclusive());

        // Tail partial: [2027-01-01, 2027-06-10)
        assertFalse(tiles.get(2).isAligned());
        assertEquals(LocalDate.of(2027, 1, 1), tiles.get(2).getStartInclusive());
        assertEquals(LocalDate.of(2027, 6, 10), tiles.get(2).getEndExclusive());
    }

    @Test
    void tile_Days_AllAligned() {
        // [Mar 15, Mar 18) by DAYS
        var tiles = TemporalTiler.<LocalDate>of(ChronoUnit.DAYS)
                .tile(LocalDate.of(2025, 3, 15), LocalDate.of(2025, 3, 18))
;

        assertEquals(3, tiles.size());
        assertTrue(tiles.stream().allMatch(TemporalTile::isAligned));
        assertEquals(LocalDate.of(2025, 3, 15), tiles.get(0).getStartInclusive());
        assertEquals(LocalDate.of(2025, 3, 16), tiles.get(0).getEndExclusive());
    }

    @Test
    void tile_Hours_PartialHead() {
        // [14:30, 17:00) by HOURS
        var tiles = TemporalTiler.<LocalDateTime>of(ChronoUnit.HOURS)
                .tile(LocalDateTime.of(2025, 3, 15, 14, 30),
                      LocalDateTime.of(2025, 3, 15, 17, 0))
;

        assertEquals(3, tiles.size());

        // Head partial: [14:30, 15:00)
        assertFalse(tiles.get(0).isAligned());
        assertEquals(LocalDateTime.of(2025, 3, 15, 14, 30), tiles.get(0).getStartInclusive());
        assertEquals(LocalDateTime.of(2025, 3, 15, 15, 0), tiles.get(0).getEndExclusive());

        // Full hour: [15:00, 16:00)
        assertTrue(tiles.get(1).isAligned());

        // Full hour: [16:00, 17:00)
        assertTrue(tiles.get(2).isAligned());
    }

    @Test
    void tile_EmptyRange() {
        var tiles = TemporalTiler.<LocalDate>of(ChronoUnit.MONTHS)
                .tile(LocalDate.of(2025, 3, 15), LocalDate.of(2025, 3, 15))
;

        assertEquals(0, tiles.size());
    }

    @Test
    void tile_SmallerThanGrain() {
        // [Mar 15, Mar 20) by MONTHS — smaller than one month
        var tiles = TemporalTiler.<LocalDate>of(ChronoUnit.MONTHS)
                .tile(LocalDate.of(2025, 3, 15), LocalDate.of(2025, 3, 20))
;

        assertEquals(1, tiles.size());
        assertFalse(tiles.get(0).isAligned());
        assertEquals(LocalDate.of(2025, 3, 15), tiles.get(0).getStartInclusive());
        assertEquals(LocalDate.of(2025, 3, 20), tiles.get(0).getEndExclusive());
    }

    @Test
    void tile_Hierarchical_UserDriven() {
        // Year → Month chaining
        var yearTiler = TemporalTiler.<LocalDate>of(ChronoUnit.YEARS);
        var monthTiler = TemporalTiler.<LocalDate>of(ChronoUnit.MONTHS);

        var start = LocalDate.of(2025, 3, 15);
        var end = LocalDate.of(2027, 6, 10);

        var yearTiles = yearTiler.tile(start, end);
        assertEquals(3, yearTiles.size()); // head partial, full 2026, tail partial

        // Decompose the head partial year into months
        var headMonths = monthTiler.tile(
                yearTiles.get(0).getStartInclusive(),
                yearTiles.get(0).getEndExclusive()
        );

        // Head partial year [2025-03-15, 2026-01-01) should have:
        // partial March, full Apr-Dec = 1 partial + 9 full = 10 tiles
        assertEquals(10, headMonths.size());
        assertFalse(headMonths.get(0).isAligned()); // partial March
        assertTrue(headMonths.get(1).isAligned());  // full April
    }

    @Test
    void tile_GapFree() {
        // Verify tiles cover the entire range with no gaps
        var start = LocalDate.of(2025, 3, 15);
        var end = LocalDate.of(2025, 8, 22);
        var tiles = TemporalTiler.<LocalDate>of(ChronoUnit.MONTHS)
                .tile(start, end);

        assertEquals(start, tiles.getFirst().getStartInclusive());
        assertEquals(end, tiles.getLast().getEndExclusive());
        for (int i = 0; i < tiles.size() - 1; i++) {
            assertEquals(tiles.get(i).getEndExclusive(), tiles.get(i + 1).getStartInclusive());
        }
    }
}
