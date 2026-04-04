package com.github.jinahya.time.temporal.tile;

import lombok.extern.slf4j.Slf4j;
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
import java.util.concurrent.ThreadLocalRandom;

import static com.github.jinahya.time.temporal.tile.TemporalTileAssert.assertTile;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ChronoTiler#tile(java.time.temporal.Temporal, java.time.temporal.Temporal, ChronoUnit)}, which
 * decomposes a half-open temporal range {@code [start, end)} into a list of non-overlapping, gap-free
 * {@link TemporalTile}s at a single {@link ChronoUnit} grain.
 *
 * @see ChronoTiler
 * @see TemporalTileAssert
 */
@Slf4j
class ChronoTiler_Tile_Test {

    @DisplayName("LocalTime")
    @Nested
    class LocalTimeTest {

        @Test
        void _MINUTES_() {
            final var grain = ChronoUnit.MINUTES;
            final var start = LocalTime.now();
            final var end = start.plusNanos(
                    ThreadLocalRandom.current().nextLong()
                    & 3_600_000_000_000L
                    | 600_000_000_000L
            );
            final var tiles = ChronoTiler.tile(start, end, grain);
            tiles.forEach(t -> {
                log.debug("tile: {}", t);
            });
            assertThat(tiles)
                    .isNotEmpty()
                    .extracting(TemporalTile::getGrain).containsOnly(grain);
            TemporalTileAssert.assertTile(tiles.getFirst()).hasStart(start);
            TemporalTileAssert.assertTile(tiles.getLast()).hasEnd(end);
        }

        @Test
        void _HOURS_() {
            final var grain = ChronoUnit.HOURS;
            final var start = LocalTime.now();
            final var end =
                    start.plusNanos(
                            ThreadLocalRandom.current().nextLong()
                            & 86_400_000_000_000L
                            | 10_800_000_000_000L
                    );
            final var tiles = ChronoTiler.tile(start, end, grain);
            tiles.forEach(t -> {
                log.debug("tile: {}", t);
            });
        }

        @DisplayName("[14:30, 17:45) by HOURS")
        @Test
        void _Hours_PartialHeadAndTail() {
            final var start = LocalTime.of(14, 30);
            final var end = LocalTime.of(17, 45);
            final var tiles = ChronoTiler.tile(start, end, ChronoUnit.HOURS);
            assertThat(tiles)
                    .hasSize(4)
                    .extracting(TemporalTile::getGrain).containsOnly(ChronoUnit.HOURS);
            assertThat(tiles)
                    .satisfies(l -> {
                        assertTile(l.getFirst())
                                .hasStart(start)
                                .hasEnd(LocalTime.of(15, 0))
                                .isNotAligned();
                        assertTile(l.get(1))
                                .hasStart(LocalTime.of(15, 0))
                                .hasEnd(LocalTime.of(16, 0))
                                .isAligned();
                        assertTile(l.get(2))
                                .hasStart(LocalTime.of(16, 0))
                                .hasEnd(LocalTime.of(17, 0))
                                .isAligned();
                        assertTile(l.getLast())
                                .hasStart(LocalTime.of(17, 0))
                                .hasEnd(end)
                                .isNotAligned();
                    });
        }

        @Test
        void _Hours_AllAligned() {
            var tiles = ChronoTiler.tile(LocalTime.of(10, 0), LocalTime.of(13, 0), ChronoUnit.HOURS);
            assertThat(tiles).hasSize(3);
            assertThat(tiles).allSatisfy(t -> assertTile(t).isAligned());
        }

        @Test
        void _Minutes_PartialHeadAndTail() {
            var tiles = ChronoTiler.tile(LocalTime.of(10, 15, 30), LocalTime.of(10, 18, 20), ChronoUnit.MINUTES);
            assertThat(tiles).hasSize(4);
            assertTile(tiles.getFirst()).isNotAligned();
            assertTile(tiles.get(1)).isAligned();
            assertTile(tiles.get(2)).isAligned();
            assertTile(tiles.getLast()).isNotAligned();
        }

        @Test
        void _EmptyRange() {
            var time = LocalTime.of(12, 0);
            var tiles = ChronoTiler.tile(time, time, ChronoUnit.HOURS);
            assertThat(tiles).isEmpty();
        }

        @Test
        void _SmallerThanGrain() {
            var tiles = ChronoTiler.tile(LocalTime.of(10, 15), LocalTime.of(10, 45), ChronoUnit.HOURS);
            assertThat(tiles).hasSize(1);
            assertTile(tiles.getFirst())
                    .hasStart(LocalTime.of(10, 15))
                    .hasEnd(LocalTime.of(10, 45))
                    .isNotAligned();
        }
    }

    @Nested
    class LocalDateTest {

        @Test
        void _Months_PartialHeadAndTail() {
            var tiles = ChronoTiler.tile(LocalDate.of(2025, 3, 15), LocalDate.of(2025, 6, 10), ChronoUnit.MONTHS);
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
            var tiles = ChronoTiler.tile(LocalDate.of(2025, 3, 15), LocalDate.of(2025, 3, 18), ChronoUnit.DAYS);
            assertThat(tiles).hasSize(3);
            assertThat(tiles).allSatisfy(t -> assertTile(t).isAligned());
        }

        @Test
        void _Years_PartialHeadAndTail() {
            var tiles = ChronoTiler.tile(LocalDate.of(2025, 3, 15), LocalDate.of(2027, 6, 10), ChronoUnit.YEARS);
            assertThat(tiles).hasSize(3);
            assertTile(tiles.getFirst()).isNotAligned();
            assertTile(tiles.get(1)).isAligned();
            assertTile(tiles.getLast()).isNotAligned();
        }

        @Test
        void _Weeks_PartialHeadAndTail() {
            var tiles = ChronoTiler.tile(LocalDate.of(2025, 3, 12), LocalDate.of(2025, 3, 27), ChronoUnit.WEEKS);
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
            var date = LocalDate.of(2025, 3, 15);
            var tiles = ChronoTiler.tile(date, date, ChronoUnit.MONTHS);
            assertThat(tiles).isEmpty();
        }

        @Test
        void _SmallerThanGrain() {
            var tiles = ChronoTiler.tile(LocalDate.of(2025, 3, 15), LocalDate.of(2025, 3, 20), ChronoUnit.MONTHS);
            assertThat(tiles).hasSize(1);
            assertTile(tiles.getFirst()).isNotAligned();
        }

        @Test
        void _GapFree() {
            var start = LocalDate.of(2025, 3, 15);
            var end = LocalDate.of(2025, 8, 22);
            var tiles = ChronoTiler.tile(start, end, ChronoUnit.MONTHS);
            assertThat(tiles.getFirst().getStart()).isEqualTo(start);
            assertThat(tiles.getLast().getEnd()).isEqualTo(end);
            for (int i = 0; i < tiles.size() - 1; i++) {
                assertThat(tiles.get(i).getEnd()).isEqualTo(tiles.get(i + 1).getStart());
            }
        }
    }

    @Nested
    class LocalDateTimeTest {

        @Test
        void _Hours_PartialHeadAndTail() {
            var tiles = ChronoTiler.tile(
                    LocalDateTime.of(2025, 3, 15, 14, 30),
                    LocalDateTime.of(2025, 3, 15, 17, 30),
                    ChronoUnit.HOURS);
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
        void _Days_PartialHeadAndTail() {
            var tiles = ChronoTiler.tile(
                    LocalDateTime.of(2025, 3, 15, 10, 0),
                    LocalDateTime.of(2025, 3, 18, 14, 0),
                    ChronoUnit.DAYS);
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
            var tiles = ChronoTiler.tile(
                    LocalDateTime.of(2025, 4, 1, 0, 0),
                    LocalDateTime.of(2025, 7, 1, 0, 0),
                    ChronoUnit.MONTHS);
            assertThat(tiles).hasSize(3);
            assertThat(tiles).allSatisfy(t -> assertTile(t).isAligned());
        }

        @Test
        void _Minutes_PartialHead() {
            var tiles = ChronoTiler.tile(
                    LocalDateTime.of(2025, 3, 15, 14, 15, 30),
                    LocalDateTime.of(2025, 3, 15, 14, 18, 0),
                    ChronoUnit.MINUTES);
            assertThat(tiles).hasSize(3);
            assertTile(tiles.getFirst()).isNotAligned();
            assertTile(tiles.get(1)).isAligned();
            assertTile(tiles.get(2)).isAligned();
        }
    }

    @Nested
    class OffsetDateTimeTest {

        @Test
        void _Hours_PartialHeadAndTail() {
            var offset = ZoneOffset.ofHours(9);
            var tiles = ChronoTiler.tile(
                    OffsetDateTime.of(2025, 3, 15, 14, 30, 0, 0, offset),
                    OffsetDateTime.of(2025, 3, 15, 17, 15, 0, 0, offset),
                    ChronoUnit.HOURS);
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
            var offset = ZoneOffset.UTC;
            var tiles = ChronoTiler.tile(
                    OffsetDateTime.of(2025, 3, 15, 0, 0, 0, 0, offset),
                    OffsetDateTime.of(2025, 3, 18, 0, 0, 0, 0, offset),
                    ChronoUnit.DAYS);
            assertThat(tiles).hasSize(3);
            assertThat(tiles).allSatisfy(t -> assertTile(t).isAligned());
        }

        @Test
        void _Months_PartialHeadAndTail() {
            var offset = ZoneOffset.ofHours(-5);
            var tiles = ChronoTiler.tile(
                    OffsetDateTime.of(2025, 3, 15, 0, 0, 0, 0, offset),
                    OffsetDateTime.of(2025, 6, 10, 0, 0, 0, 0, offset),
                    ChronoUnit.MONTHS);
            assertThat(tiles).hasSize(4);
            assertTile(tiles.getFirst()).isNotAligned();
            assertTile(tiles.get(1)).isAligned();
            assertTile(tiles.get(2)).isAligned();
            assertTile(tiles.getLast()).isNotAligned();
        }
    }

    @Nested
    class ZonedDateTimeTest {

        @Test
        void _Hours_PartialHeadAndTail() {
            var zone = ZoneId.of("America/New_York");
            var tiles = ChronoTiler.tile(
                    ZonedDateTime.of(2025, 3, 15, 14, 30, 0, 0, zone),
                    ZonedDateTime.of(2025, 3, 15, 17, 45, 0, 0, zone),
                    ChronoUnit.HOURS);
            assertThat(tiles).hasSize(4);
            assertTile(tiles.getFirst()).isNotAligned();
            assertTile(tiles.get(1)).isAligned();
            assertTile(tiles.get(2)).isAligned();
            assertTile(tiles.getLast()).isNotAligned();
        }

        @Test
        void _Days_PartialHead() {
            var zone = ZoneId.of("Europe/London");
            var tiles = ChronoTiler.tile(
                    ZonedDateTime.of(2025, 3, 15, 10, 0, 0, 0, zone),
                    ZonedDateTime.of(2025, 3, 18, 0, 0, 0, 0, zone),
                    ChronoUnit.DAYS);
            assertThat(tiles).hasSize(3);
            assertTile(tiles.getFirst()).isNotAligned();
            assertTile(tiles.get(1)).isAligned();
            assertTile(tiles.get(2)).isAligned();
        }

        @Test
        void _Months_AllAligned() {
            var zone = ZoneId.of("Asia/Tokyo");
            var tiles = ChronoTiler.tile(
                    ZonedDateTime.of(2025, 4, 1, 0, 0, 0, 0, zone),
                    ZonedDateTime.of(2025, 7, 1, 0, 0, 0, 0, zone),
                    ChronoUnit.MONTHS);
            assertThat(tiles).hasSize(3);
            assertThat(tiles).allSatisfy(t -> assertTile(t).isAligned());
        }

        @Test
        void _EmptyRange() {
            var zone = ZoneId.of("UTC");
            var zdt = ZonedDateTime.of(2025, 3, 15, 12, 0, 0, 0, zone);
            var tiles = ChronoTiler.tile(zdt, zdt, ChronoUnit.HOURS);
            assertThat(tiles).isEmpty();
        }
    }

    @Test
    void _Months_PartialHeadAndTail() {
        var tiles = ChronoTiler.tile(LocalDate.of(2025, 3, 15), LocalDate.of(2025, 6, 10), ChronoUnit.MONTHS);
        assertThat(tiles).hasSize(4);
        assertTile(tiles.getFirst())
                .hasStart(LocalDate.of(2025, 3, 15))
                .hasEnd(LocalDate.of(2025, 4, 1))
                .isNotAligned();
        assertTile(tiles.get(1))
                .hasStart(LocalDate.of(2025, 4, 1))
                .hasEnd(LocalDate.of(2025, 5, 1))
                .isAligned();
        assertTile(tiles.get(2))
                .hasStart(LocalDate.of(2025, 5, 1))
                .hasEnd(LocalDate.of(2025, 6, 1))
                .isAligned();
        assertTile(tiles.getLast())
                .hasStart(LocalDate.of(2025, 6, 1))
                .hasEnd(LocalDate.of(2025, 6, 10))
                .isNotAligned();
    }

    @Test
    void _Months_AlignedStartAndEnd() {
        var tiles = ChronoTiler.tile(LocalDate.of(2025, 4, 1), LocalDate.of(2025, 7, 1), ChronoUnit.MONTHS);
        assertThat(tiles).hasSize(3);
        assertThat(tiles).allSatisfy(t -> assertTile(t).isAligned());
    }

    @Test
    void _Years_PartialHeadAndTail() {
        var tiles = ChronoTiler.tile(LocalDate.of(2025, 3, 15), LocalDate.of(2027, 6, 10), ChronoUnit.YEARS);
        assertThat(tiles).hasSize(3);
        assertTile(tiles.getFirst())
                .hasStart(LocalDate.of(2025, 3, 15))
                .hasEnd(LocalDate.of(2026, 1, 1))
                .isNotAligned();
        assertTile(tiles.get(1))
                .hasStart(LocalDate.of(2026, 1, 1))
                .hasEnd(LocalDate.of(2027, 1, 1))
                .isAligned();
        assertTile(tiles.getLast())
                .hasStart(LocalDate.of(2027, 1, 1))
                .hasEnd(LocalDate.of(2027, 6, 10))
                .isNotAligned();
    }

    @Test
    void _Days_AllAligned() {
        var tiles = ChronoTiler.tile(LocalDate.of(2025, 3, 15), LocalDate.of(2025, 3, 18), ChronoUnit.DAYS);
        assertThat(tiles).hasSize(3);
        assertThat(tiles).allSatisfy(t -> assertTile(t).isAligned());
        assertTile(tiles.getFirst())
                .hasStart(LocalDate.of(2025, 3, 15))
                .hasEnd(LocalDate.of(2025, 3, 16));
    }

    @Test
    void _Hours_PartialHead() {
        var tiles = ChronoTiler.tile(
                LocalDateTime.of(2025, 3, 15, 14, 30),
                LocalDateTime.of(2025, 3, 15, 17, 0),
                ChronoUnit.HOURS);
        assertThat(tiles).hasSize(3);
        assertTile(tiles.getFirst())
                .hasStart(LocalDateTime.of(2025, 3, 15, 14, 30))
                .hasEnd(LocalDateTime.of(2025, 3, 15, 15, 0))
                .isNotAligned();
        assertTile(tiles.get(1)).isAligned();
        assertTile(tiles.get(2)).isAligned();
    }

    @Test
    void _EmptyRange() {
        var tiles = ChronoTiler.tile(LocalDate.of(2025, 3, 15), LocalDate.of(2025, 3, 15), ChronoUnit.MONTHS);
        assertThat(tiles).isEmpty();
    }

    @Test
    void _SmallerThanGrain() {
        var tiles = ChronoTiler.tile(LocalDate.of(2025, 3, 15), LocalDate.of(2025, 3, 20), ChronoUnit.MONTHS);
        assertThat(tiles).hasSize(1);
        assertTile(tiles.getFirst())
                .hasStart(LocalDate.of(2025, 3, 15))
                .hasEnd(LocalDate.of(2025, 3, 20))
                .isNotAligned();
    }

    @Test
    void _Hierarchical_UserDriven() {
        var start = LocalDate.of(2025, 3, 15);
        var end = LocalDate.of(2027, 6, 10);

        var yearTiles = ChronoTiler.tile(start, end, ChronoUnit.YEARS);
        assertThat(yearTiles).hasSize(3);

        var headMonths = ChronoTiler.tile(
                yearTiles.getFirst().getStart(),
                yearTiles.getFirst().getEnd(),
                ChronoUnit.MONTHS);

        assertThat(headMonths).hasSize(10);
        assertTile(headMonths.getFirst()).isNotAligned();
        assertTile(headMonths.get(1)).isAligned();
    }

    @Test
    void _GapFree() {
        var start = LocalDate.of(2025, 3, 15);
        var end = LocalDate.of(2025, 8, 22);
        var tiles = ChronoTiler.tile(start, end, ChronoUnit.MONTHS);
        assertThat(tiles.getFirst().getStart()).isEqualTo(start);
        assertThat(tiles.getLast().getEnd()).isEqualTo(end);
        for (int i = 0; i < tiles.size() - 1; i++) {
            assertThat(tiles.get(i).getEnd()).isEqualTo(tiles.get(i + 1).getStart());
        }
    }
}
