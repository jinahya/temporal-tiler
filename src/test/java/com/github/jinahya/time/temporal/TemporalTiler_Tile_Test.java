package com.github.jinahya.time.temporal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A class for testing {@link TemporalTiler#tile(Temporal, Temporal)} method.
 */
class TemporalTiler_Tile_Test {

    @DisplayName("LocalTime")
    @Nested
    class LocalTimeTest {

        @Test
        void _Hours_PartialHeadAndTail() {
            // [14:30, 17:45) by HOURS
            var tiles = TemporalTiler.<LocalTime>of(ChronoUnit.HOURS)
                    .tile(LocalTime.of(14, 30), LocalTime.of(17, 45));

            assertEquals(4, tiles.size());

            // Head partial: [14:30, 15:00)
            assertFalse(tiles.getFirst().isAligned());
            assertEquals(LocalTime.of(14, 30), tiles.get(0).getStartInclusive());
            assertEquals(LocalTime.of(15, 0), tiles.get(0).getEndExclusive());

            // Full hours: [15:00, 16:00), [16:00, 17:00)
            assertTrue(tiles.get(1).isAligned());
            assertTrue(tiles.get(2).isAligned());

            // Tail partial: [17:00, 17:45)
            assertFalse(tiles.get(3).isAligned());
            assertEquals(LocalTime.of(17, 0), tiles.get(3).getStartInclusive());
            assertEquals(LocalTime.of(17, 45), tiles.get(3).getEndExclusive());
        }

        @Test
        void _Hours_AllAligned() {
            // [10:00, 13:00) by HOURS
            var tiles = TemporalTiler.<LocalTime>of(ChronoUnit.HOURS)
                    .tile(LocalTime.of(10, 0), LocalTime.of(13, 0));

            assertEquals(3, tiles.size());
            assertTrue(tiles.stream().allMatch(TemporalTile::isAligned));
        }

        @Test
        void _Minutes_PartialHeadAndTail() {
            // [10:15:30, 10:18:20) by MINUTES
            var tiles = TemporalTiler.<LocalTime>of(ChronoUnit.MINUTES)
                    .tile(LocalTime.of(10, 15, 30), LocalTime.of(10, 18, 20));

            assertEquals(4, tiles.size());

            // Head partial: [10:15:30, 10:16:00)
            assertFalse(tiles.get(0).isAligned());

            // Full minutes: [10:16, 10:17), [10:17, 10:18)
            assertTrue(tiles.get(1).isAligned());
            assertTrue(tiles.get(2).isAligned());

            // Tail partial: [10:18:00, 10:18:20)
            assertFalse(tiles.get(3).isAligned());
        }

        @Test
        void _EmptyRange() {
            var time = LocalTime.of(12, 0);
            var tiles = TemporalTiler.<LocalTime>of(ChronoUnit.HOURS)
                    .tile(time, time);

            assertEquals(0, tiles.size());
        }

        @Test
        void _SmallerThanGrain() {
            // [10:15, 10:45) by HOURS — smaller than one hour
            var tiles = TemporalTiler.<LocalTime>of(ChronoUnit.HOURS)
                    .tile(LocalTime.of(10, 15), LocalTime.of(10, 45));

            assertEquals(1, tiles.size());
            assertFalse(tiles.get(0).isAligned());
            assertEquals(LocalTime.of(10, 15), tiles.get(0).getStartInclusive());
            assertEquals(LocalTime.of(10, 45), tiles.get(0).getEndExclusive());
        }
    }

    @Nested
    class LocalDateTest {

        @Test
        void _Months_PartialHeadAndTail() {
            // [Mar 15, Jun 10) by MONTHS
            var tiles = TemporalTiler.<LocalDate>of(ChronoUnit.MONTHS)
                    .tile(LocalDate.of(2025, 3, 15), LocalDate.of(2025, 6, 10));

            assertEquals(4, tiles.size());

            // Head partial: [Mar 15, Apr 1)
            assertFalse(tiles.get(0).isAligned());
            assertEquals(LocalDate.of(2025, 3, 15), tiles.get(0).getStartInclusive());
            assertEquals(LocalDate.of(2025, 4, 1), tiles.get(0).getEndExclusive());

            // Full months: [Apr 1, May 1), [May 1, Jun 1)
            assertTrue(tiles.get(1).isAligned());
            assertTrue(tiles.get(2).isAligned());

            // Tail partial: [Jun 1, Jun 10)
            assertFalse(tiles.get(3).isAligned());
            assertEquals(LocalDate.of(2025, 6, 1), tiles.get(3).getStartInclusive());
            assertEquals(LocalDate.of(2025, 6, 10), tiles.get(3).getEndExclusive());
        }

        @Test
        void _Days_AllAligned() {
            // [Mar 15, Mar 18) by DAYS — LocalDate days are always aligned
            var tiles = TemporalTiler.<LocalDate>of(ChronoUnit.DAYS)
                    .tile(LocalDate.of(2025, 3, 15), LocalDate.of(2025, 3, 18));

            assertEquals(3, tiles.size());
            assertTrue(tiles.stream().allMatch(TemporalTile::isAligned));
        }

        @Test
        void _Years_PartialHeadAndTail() {
            var tiles = TemporalTiler.<LocalDate>of(ChronoUnit.YEARS)
                    .tile(LocalDate.of(2025, 3, 15), LocalDate.of(2027, 6, 10));

            assertEquals(3, tiles.size());

            // Head partial: [2025-03-15, 2026-01-01)
            assertFalse(tiles.get(0).isAligned());

            // Full year: [2026-01-01, 2027-01-01)
            assertTrue(tiles.get(1).isAligned());

            // Tail partial: [2027-01-01, 2027-06-10)
            assertFalse(tiles.get(2).isAligned());
        }

        @Test
        void _Weeks_PartialHeadAndTail() {
            // [Wed Mar 12, Thu Mar 27) by WEEKS (week starts Monday)
            var tiles = TemporalTiler.<LocalDate>of(ChronoUnit.WEEKS)
                    .tile(LocalDate.of(2025, 3, 12), LocalDate.of(2025, 3, 27));

            assertEquals(3, tiles.size());

            // Head partial: [Wed Mar 12, Mon Mar 17)
            assertFalse(tiles.get(0).isAligned());
            assertEquals(LocalDate.of(2025, 3, 12), tiles.get(0).getStartInclusive());
            assertEquals(LocalDate.of(2025, 3, 17), tiles.get(0).getEndExclusive());

            // Full week: [Mon Mar 17, Mon Mar 24)
            assertTrue(tiles.get(1).isAligned());

            // Tail partial: [Mon Mar 24, Thu Mar 27)
            assertFalse(tiles.get(2).isAligned());
        }

        @Test
        void _EmptyRange() {
            var date = LocalDate.of(2025, 3, 15);
            var tiles = TemporalTiler.<LocalDate>of(ChronoUnit.MONTHS)
                    .tile(date, date);

            assertEquals(0, tiles.size());
        }

        @Test
        void _SmallerThanGrain() {
            // [Mar 15, Mar 20) by MONTHS — smaller than one month
            var tiles = TemporalTiler.<LocalDate>of(ChronoUnit.MONTHS)
                    .tile(LocalDate.of(2025, 3, 15), LocalDate.of(2025, 3, 20));

            assertEquals(1, tiles.size());
            assertFalse(tiles.get(0).isAligned());
        }

        @Test
        void _GapFree() {
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

    @Nested
    class LocalDateTimeTest {

        @Test
        void _Hours_PartialHeadAndTail() {
            // [14:30, 17:30) by HOURS
            var tiles = TemporalTiler.<LocalDateTime>of(ChronoUnit.HOURS)
                    .tile(LocalDateTime.of(2025, 3, 15, 14, 30),
                          LocalDateTime.of(2025, 3, 15, 17, 30));

            assertEquals(4, tiles.size());

            // Head partial: [14:30, 15:00)
            assertFalse(tiles.get(0).isAligned());
            assertEquals(LocalDateTime.of(2025, 3, 15, 14, 30), tiles.get(0).getStartInclusive());
            assertEquals(LocalDateTime.of(2025, 3, 15, 15, 0), tiles.get(0).getEndExclusive());

            // Full hours: [15:00, 16:00), [16:00, 17:00)
            assertTrue(tiles.get(1).isAligned());
            assertTrue(tiles.get(2).isAligned());

            // Tail partial: [17:00, 17:30)
            assertFalse(tiles.get(3).isAligned());
            assertEquals(LocalDateTime.of(2025, 3, 15, 17, 0), tiles.get(3).getStartInclusive());
            assertEquals(LocalDateTime.of(2025, 3, 15, 17, 30), tiles.get(3).getEndExclusive());
        }

        @Test
        void _Days_PartialHeadAndTail() {
            // [Mar 15 10:00, Mar 18 14:00) by DAYS
            var tiles = TemporalTiler.<LocalDateTime>of(ChronoUnit.DAYS)
                    .tile(LocalDateTime.of(2025, 3, 15, 10, 0),
                          LocalDateTime.of(2025, 3, 18, 14, 0));

            assertEquals(4, tiles.size());

            // Head partial: [Mar 15 10:00, Mar 16 00:00)
            assertFalse(tiles.get(0).isAligned());
            assertEquals(LocalDateTime.of(2025, 3, 15, 10, 0), tiles.get(0).getStartInclusive());
            assertEquals(LocalDateTime.of(2025, 3, 16, 0, 0), tiles.get(0).getEndExclusive());

            // Full days: [Mar 16, Mar 17), [Mar 17, Mar 18)
            assertTrue(tiles.get(1).isAligned());
            assertTrue(tiles.get(2).isAligned());

            // Tail partial: [Mar 18 00:00, Mar 18 14:00)
            assertFalse(tiles.get(3).isAligned());
        }

        @Test
        void _Months_AllAligned() {
            // [Apr 1 00:00, Jul 1 00:00) by MONTHS
            var tiles = TemporalTiler.<LocalDateTime>of(ChronoUnit.MONTHS)
                    .tile(LocalDateTime.of(2025, 4, 1, 0, 0),
                          LocalDateTime.of(2025, 7, 1, 0, 0));

            assertEquals(3, tiles.size());
            assertTrue(tiles.stream().allMatch(TemporalTile::isAligned));
        }

        @Test
        void _Minutes_PartialHead() {
            // [14:15:30, 14:18:00) by MINUTES
            var tiles = TemporalTiler.<LocalDateTime>of(ChronoUnit.MINUTES)
                    .tile(LocalDateTime.of(2025, 3, 15, 14, 15, 30),
                          LocalDateTime.of(2025, 3, 15, 14, 18, 0));

            assertEquals(3, tiles.size());

            // Head partial: [14:15:30, 14:16:00)
            assertFalse(tiles.get(0).isAligned());

            // Full minutes: [14:16, 14:17), [14:17, 14:18)
            assertTrue(tiles.get(1).isAligned());
            assertTrue(tiles.get(2).isAligned());
        }
    }

    @Nested
    class OffsetDateTimeTest {

        @Test
        void _Hours_PartialHeadAndTail() {
            var offset = ZoneOffset.ofHours(9);
            var tiles = TemporalTiler.<OffsetDateTime>of(ChronoUnit.HOURS)
                    .tile(OffsetDateTime.of(2025, 3, 15, 14, 30, 0, 0, offset),
                          OffsetDateTime.of(2025, 3, 15, 17, 15, 0, 0, offset));

            assertEquals(4, tiles.size());

            // Head partial: [14:30, 15:00)
            assertFalse(tiles.get(0).isAligned());
            assertEquals(OffsetDateTime.of(2025, 3, 15, 14, 30, 0, 0, offset),
                         tiles.get(0).getStartInclusive());
            assertEquals(OffsetDateTime.of(2025, 3, 15, 15, 0, 0, 0, offset),
                         tiles.get(0).getEndExclusive());

            // Full hours: [15:00, 16:00), [16:00, 17:00)
            assertTrue(tiles.get(1).isAligned());
            assertTrue(tiles.get(2).isAligned());

            // Tail partial: [17:00, 17:15)
            assertFalse(tiles.get(3).isAligned());
        }

        @Test
        void _Days_AllAligned() {
            var offset = ZoneOffset.UTC;
            var tiles = TemporalTiler.<OffsetDateTime>of(ChronoUnit.DAYS)
                    .tile(OffsetDateTime.of(2025, 3, 15, 0, 0, 0, 0, offset),
                          OffsetDateTime.of(2025, 3, 18, 0, 0, 0, 0, offset));

            assertEquals(3, tiles.size());
            assertTrue(tiles.stream().allMatch(TemporalTile::isAligned));
        }

        @Test
        void _Months_PartialHeadAndTail() {
            var offset = ZoneOffset.ofHours(-5);
            var tiles = TemporalTiler.<OffsetDateTime>of(ChronoUnit.MONTHS)
                    .tile(OffsetDateTime.of(2025, 3, 15, 0, 0, 0, 0, offset),
                          OffsetDateTime.of(2025, 6, 10, 0, 0, 0, 0, offset));

            assertEquals(4, tiles.size());
            assertFalse(tiles.get(0).isAligned());
            assertTrue(tiles.get(1).isAligned());
            assertTrue(tiles.get(2).isAligned());
            assertFalse(tiles.get(3).isAligned());
        }
    }

    @Nested
    class ZonedDateTimeTest {

        @Test
        void _Hours_PartialHeadAndTail() {
            var zone = ZoneId.of("America/New_York");
            var tiles = TemporalTiler.<ZonedDateTime>of(ChronoUnit.HOURS)
                    .tile(ZonedDateTime.of(2025, 3, 15, 14, 30, 0, 0, zone),
                          ZonedDateTime.of(2025, 3, 15, 17, 45, 0, 0, zone));

            assertEquals(4, tiles.size());

            // Head partial: [14:30, 15:00)
            assertFalse(tiles.get(0).isAligned());

            // Full hours: [15:00, 16:00), [16:00, 17:00)
            assertTrue(tiles.get(1).isAligned());
            assertTrue(tiles.get(2).isAligned());

            // Tail partial: [17:00, 17:45)
            assertFalse(tiles.get(3).isAligned());
        }

        @Test
        void _Days_PartialHead() {
            var zone = ZoneId.of("Europe/London");
            var tiles = TemporalTiler.<ZonedDateTime>of(ChronoUnit.DAYS)
                    .tile(ZonedDateTime.of(2025, 3, 15, 10, 0, 0, 0, zone),
                          ZonedDateTime.of(2025, 3, 18, 0, 0, 0, 0, zone));

            assertEquals(3, tiles.size());

            // Head partial: [Mar 15 10:00, Mar 16 00:00)
            assertFalse(tiles.get(0).isAligned());

            // Full days: [Mar 16, Mar 17), [Mar 17, Mar 18)
            assertTrue(tiles.get(1).isAligned());
            assertTrue(tiles.get(2).isAligned());
        }

        @Test
        void _Months_AllAligned() {
            var zone = ZoneId.of("Asia/Tokyo");
            var tiles = TemporalTiler.<ZonedDateTime>of(ChronoUnit.MONTHS)
                    .tile(ZonedDateTime.of(2025, 4, 1, 0, 0, 0, 0, zone),
                          ZonedDateTime.of(2025, 7, 1, 0, 0, 0, 0, zone));

            assertEquals(3, tiles.size());
            assertTrue(tiles.stream().allMatch(TemporalTile::isAligned));
        }

        @Test
        void _EmptyRange() {
            var zone = ZoneId.of("UTC");
            var zdt = ZonedDateTime.of(2025, 3, 15, 12, 0, 0, 0, zone);
            var tiles = TemporalTiler.<ZonedDateTime>of(ChronoUnit.HOURS)
                    .tile(zdt, zdt);

            assertEquals(0, tiles.size());
        }
    }

    @Test
    void _Months_PartialHeadAndTail() {
        // [Mar 15, Jun 10) by MONTHS
        var tiles = TemporalTiler.<LocalDate>of(ChronoUnit.MONTHS)
                .tile(LocalDate.of(2025, 3, 15), LocalDate.of(2025, 6, 10));

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
    void _Months_AlignedStartAndEnd() {
        // [Apr 1, Jul 1) by MONTHS — perfectly aligned
        var tiles = TemporalTiler.<LocalDate>of(ChronoUnit.MONTHS)
                .tile(LocalDate.of(2025, 4, 1), LocalDate.of(2025, 7, 1));

        assertEquals(3, tiles.size());
        assertTrue(tiles.stream().allMatch(TemporalTile::isAligned));
    }

    @Test
    void _Years_PartialHeadAndTail() {
        // [Mar 15, Jun 10 next year) by YEARS
        var tiles = TemporalTiler.<LocalDate>of(ChronoUnit.YEARS)
                .tile(LocalDate.of(2025, 3, 15), LocalDate.of(2027, 6, 10));

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
    void _Days_AllAligned() {
        // [Mar 15, Mar 18) by DAYS
        var tiles = TemporalTiler.<LocalDate>of(ChronoUnit.DAYS)
                .tile(LocalDate.of(2025, 3, 15), LocalDate.of(2025, 3, 18));

        assertEquals(3, tiles.size());
        assertTrue(tiles.stream().allMatch(TemporalTile::isAligned));
        assertEquals(LocalDate.of(2025, 3, 15), tiles.get(0).getStartInclusive());
        assertEquals(LocalDate.of(2025, 3, 16), tiles.get(0).getEndExclusive());
    }

    @Test
    void _Hours_PartialHead() {
        // [14:30, 17:00) by HOURS
        var tiles = TemporalTiler.<LocalDateTime>of(ChronoUnit.HOURS)
                .tile(LocalDateTime.of(2025, 3, 15, 14, 30),
                      LocalDateTime.of(2025, 3, 15, 17, 0));

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
    void _EmptyRange() {
        var tiles = TemporalTiler.<LocalDate>of(ChronoUnit.MONTHS)
                .tile(LocalDate.of(2025, 3, 15), LocalDate.of(2025, 3, 15));

        assertEquals(0, tiles.size());
    }

    @Test
    void _SmallerThanGrain() {
        // [Mar 15, Mar 20) by MONTHS — smaller than one month
        var tiles = TemporalTiler.<LocalDate>of(ChronoUnit.MONTHS)
                .tile(LocalDate.of(2025, 3, 15), LocalDate.of(2025, 3, 20));

        assertEquals(1, tiles.size());
        assertFalse(tiles.get(0).isAligned());
        assertEquals(LocalDate.of(2025, 3, 15), tiles.get(0).getStartInclusive());
        assertEquals(LocalDate.of(2025, 3, 20), tiles.get(0).getEndExclusive());
    }

    @Test
    void _Hierarchical_UserDriven() {
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
    void _GapFree() {
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
