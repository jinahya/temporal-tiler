package com.github.jinahya.time.temporal;

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

import static com.github.jinahya.time.temporal.TemporalTileAssert.asserTile;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@link TemporalTiler#tile()} method, which decomposes a half-open temporal range {@code [start, end)}
 * into a list of non-overlapping, gap-free {@link TemporalTile}s at a single {@link ChronoUnit} grain.
 *
 * <p>The test class is organized into {@link Nested} inner classes by temporal type:
 * <ul>
 *     <li>{@link LocalTimeTest} &mdash; time-only tiling with {@link LocalTime} ({@code HOURS}, {@code MINUTES})</li>
 *     <li>{@link LocalDateTest} &mdash; date-only tiling with {@link LocalDate}
 *         ({@code DAYS}, {@code WEEKS}, {@code MONTHS}, {@code YEARS})</li>
 *     <li>{@link LocalDateTimeTest} &mdash; date-time tiling with {@link LocalDateTime}
 *         ({@code MINUTES}, {@code HOURS}, {@code DAYS}, {@code MONTHS})</li>
 *     <li>{@link OffsetDateTimeTest} &mdash; offset-aware tiling with {@link OffsetDateTime}
 *         ({@code HOURS}, {@code DAYS}, {@code MONTHS})</li>
 *     <li>{@link ZonedDateTimeTest} &mdash; zone-aware tiling with {@link ZonedDateTime}
 *         ({@code HOURS}, {@code DAYS}, {@code MONTHS})</li>
 * </ul>
 *
 * <p>Additional top-level test methods cover cross-cutting concerns such as:
 * <ul>
 *     <li>Partial head and tail tiles when the range boundaries do not align with the grain</li>
 *     <li>Fully aligned ranges where every tile is boundary-aligned</li>
 *     <li>Empty ranges ({@code start == end}) producing an empty list</li>
 *     <li>Ranges smaller than one grain unit producing a single partial tile</li>
 *     <li>Hierarchical (user-driven) tiling by chaining tilers at different grains</li>
 *     <li>Gap-free verification: the end of each tile equals the start of the next</li>
 * </ul>
 *
 * <p>Assertions on individual tiles use {@link TemporalTileAssert} for fluent, readable checks on
 * {@link TemporalTile#getStart() start}, {@link TemporalTile#getEnd() end}, and
 * {@link TemporalTile#isAligned() alignment}.
 *
 * @see TemporalTiler#tile()
 * @see TemporalTile
 * @see TemporalTileAssert
 */
@Slf4j
class TemporalTiler_Tile_Test {

    @DisplayName("LocalTime")
    @Nested
    class LocalTimeTest {

        @Test
        void _MINUTES_() {
            // --------------------------------------------------------------------------------------------------- given
            final var grain = ChronoUnit.MINUTES;
            final var start = LocalTime.now();
            final var end = start.plusNanos(
                    ThreadLocalRandom.current().nextLong()
                    & 3_600_000_000_000L //  1 hour
                    | 600_000_000_000L   // 10 minute
            );
            // ---------------------------------------------------------------------------------------------------- when
            final var tiles = new TemporalTiler.Builder<LocalTime>()
                    .start(start).end(end).grain(grain)
                    .build().tile();
            tiles.forEach(t -> {
                log.debug("tile: {}", t);
            });
            assertThat(tiles)
                    .isNotEmpty()
                    .extracting(TemporalTile::getGrain).containsOnly(grain);
            TemporalTileAssert.asserTile(tiles.getFirst()).hasStart(start);
            TemporalTileAssert.asserTile(tiles.getLast()).hasEnd(end);
        }

        @Test
        void _HOURS_() {
            // --------------------------------------------------------------------------------------------------- given
            final var grain = ChronoUnit.HOURS;
            final var start = LocalTime.now();
            final var end =
                    start.plusNanos(
                            ThreadLocalRandom.current().nextLong()
                            & 86_400_000_000_000L // 24 hours
                            | 10_800_000_000_000L //  3 hours
                    );
            // ---------------------------------------------------------------------------------------------------- when
            final var tiles = new TemporalTiler.Builder<LocalTime>()
                    .start(start).end(end).grain(grain)
                    .build().tile();
            tiles.forEach(t -> {
                log.debug("tile: {}", t);
            });
        }

        @DisplayName("[14:30, 17:45) by HOURS")
        @Test
        void _Hours_PartialHeadAndTail() {
            // --------------------------------------------------------------------------------------------------- given
            final var grain = ChronoUnit.HOURS;
            final var start = LocalTime.of(14, 30);
            final var end = LocalTime.of(17, 45);
            // ---------------------------------------------------------------------------------------------------- when
            final var tiles = new TemporalTiler.Builder<LocalTime>()
                    .start(start)
                    .end(end)
                    .grain(grain)
                    .build()
                    .tile();
            // ---------------------------------------------------------------------------------------------------- then
            assertThat(tiles)
                    .hasSize(4)
                    .extracting(TemporalTile::getGrain).containsOnly(ChronoUnit.HOURS);
            assertThat(tiles)
                    .satisfies(l -> {
                        asserTile(l.getFirst())
                                .hasStart(start)
                                .hasEnd(LocalTime.of(15, 0))
                                .isNotAligned();
                        asserTile(l.get(1))
                                .hasStart(LocalTime.of(15, 0))
                                .hasEnd(LocalTime.of(16, 0))
                                .isAligned();
                        asserTile(l.get(2))
                                .hasStart(LocalTime.of(16, 0))
                                .hasEnd(LocalTime.of(17, 0))
                                .isAligned();
                        asserTile(l.getLast())
                                .hasStart(LocalTime.of(17, 0))
                                .hasEnd(end)
                                .isNotAligned();
                    });
        }

        @Test
        void _Hours_AllAligned() {
            // [10:00, 13:00) by HOURS
            var tiles = new TemporalTiler.Builder<LocalTime>()
                    .start(LocalTime.of(10, 0)).end(LocalTime.of(13, 0)).grain(ChronoUnit.HOURS)
                    .build().tile();
            assertThat(tiles).hasSize(3);
            assertThat(tiles).allSatisfy(t -> asserTile(t).isAligned());
        }

        @Test
        void _Minutes_PartialHeadAndTail() {
            // [10:15:30, 10:18:20) by MINUTES
            var tiles = new TemporalTiler.Builder<LocalTime>()
                    .start(LocalTime.of(10, 15, 30)).end(LocalTime.of(10, 18, 20)).grain(ChronoUnit.MINUTES)
                    .build().tile();
            assertThat(tiles).hasSize(4);
            // Head partial: [10:15:30, 10:16:00)
            asserTile(tiles.getFirst()).isNotAligned();
            // Full minutes: [10:16, 10:17), [10:17, 10:18)
            asserTile(tiles.get(1)).isAligned();
            asserTile(tiles.get(2)).isAligned();
            // Tail partial: [10:18:00, 10:18:20)
            asserTile(tiles.getLast()).isNotAligned();
        }

        @Test
        void _EmptyRange() {
            var time = LocalTime.of(12, 0);
            var tiles = new TemporalTiler.Builder<LocalTime>()
                    .start(time).end(time).grain(ChronoUnit.HOURS)
                    .build().tile();
            assertThat(tiles).isEmpty();
        }

        @Test
        void _SmallerThanGrain() {
            // [10:15, 10:45) by HOURS — smaller than one hour
            var tiles = new TemporalTiler.Builder<LocalTime>()
                    .start(LocalTime.of(10, 15)).end(LocalTime.of(10, 45)).grain(ChronoUnit.HOURS)
                    .build().tile();
            assertThat(tiles).hasSize(1);
            asserTile(tiles.getFirst())
                    .hasStart(LocalTime.of(10, 15))
                    .hasEnd(LocalTime.of(10, 45))
                    .isNotAligned();
        }
    }

    @Nested
    class LocalDateTest {

        @Test
        void _Months_PartialHeadAndTail() {
            // [Mar 15, Jun 10) by MONTHS
            var tiles = new TemporalTiler.Builder<LocalDate>()
                    .start(LocalDate.of(2025, 3, 15)).end(LocalDate.of(2025, 6, 10)).grain(ChronoUnit.MONTHS)
                    .build().tile();
            assertThat(tiles).hasSize(4);
            // Head partial: [Mar 15, Apr 1)
            asserTile(tiles.getFirst())
                    .hasStart(LocalDate.of(2025, 3, 15))
                    .hasEnd(LocalDate.of(2025, 4, 1))
                    .isNotAligned();
            // Full months: [Apr 1, May 1), [May 1, Jun 1)
            asserTile(tiles.get(1)).isAligned();
            asserTile(tiles.get(2)).isAligned();
            // Tail partial: [Jun 1, Jun 10)
            asserTile(tiles.getLast())
                    .hasStart(LocalDate.of(2025, 6, 1))
                    .hasEnd(LocalDate.of(2025, 6, 10))
                    .isNotAligned();
        }

        @Test
        void _Days_AllAligned() {
            // [Mar 15, Mar 18) by DAYS — LocalDate days are always aligned
            var tiles = new TemporalTiler.Builder<LocalDate>()
                    .start(LocalDate.of(2025, 3, 15)).end(LocalDate.of(2025, 3, 18)).grain(ChronoUnit.DAYS)
                    .build().tile();
            assertThat(tiles).hasSize(3);
            assertThat(tiles).allSatisfy(t -> asserTile(t).isAligned());
        }

        @Test
        void _Years_PartialHeadAndTail() {
            var tiles = new TemporalTiler.Builder<LocalDate>()
                    .start(LocalDate.of(2025, 3, 15)).end(LocalDate.of(2027, 6, 10)).grain(ChronoUnit.YEARS)
                    .build().tile();
            assertThat(tiles).hasSize(3);
            // Head partial: [2025-03-15, 2026-01-01)
            asserTile(tiles.getFirst()).isNotAligned();
            // Full year: [2026-01-01, 2027-01-01)
            asserTile(tiles.get(1)).isAligned();
            // Tail partial: [2027-01-01, 2027-06-10)
            asserTile(tiles.getLast()).isNotAligned();
        }

        @Test
        void _Weeks_PartialHeadAndTail() {
            // [Wed Mar 12, Thu Mar 27) by WEEKS (week starts Monday)
            var tiles = new TemporalTiler.Builder<LocalDate>()
                    .start(LocalDate.of(2025, 3, 12)).end(LocalDate.of(2025, 3, 27)).grain(ChronoUnit.WEEKS)
                    .build().tile();
            assertThat(tiles).hasSize(3);
            // Head partial: [Wed Mar 12, Mon Mar 17)
            asserTile(tiles.getFirst())
                    .hasStart(LocalDate.of(2025, 3, 12))
                    .hasEnd(LocalDate.of(2025, 3, 17))
                    .isNotAligned();
            // Full week: [Mon Mar 17, Mon Mar 24)
            asserTile(tiles.get(1)).isAligned();
            // Tail partial: [Mon Mar 24, Thu Mar 27)
            asserTile(tiles.getLast()).isNotAligned();
        }

        @Test
        void _EmptyRange() {
            var date = LocalDate.of(2025, 3, 15);
            var tiles = new TemporalTiler.Builder<LocalDate>()
                    .start(date).end(date).grain(ChronoUnit.MONTHS)
                    .build().tile();
            assertThat(tiles).isEmpty();
        }

        @Test
        void _SmallerThanGrain() {
            // [Mar 15, Mar 20) by MONTHS — smaller than one month
            var tiles = new TemporalTiler.Builder<LocalDate>()
                    .start(LocalDate.of(2025, 3, 15)).end(LocalDate.of(2025, 3, 20)).grain(ChronoUnit.MONTHS)
                    .build().tile();
            assertThat(tiles).hasSize(1);
            asserTile(tiles.getFirst()).isNotAligned();
        }

        @Test
        void _GapFree() {
            var start = LocalDate.of(2025, 3, 15);
            var end = LocalDate.of(2025, 8, 22);
            var tiles = new TemporalTiler.Builder<LocalDate>()
                    .start(start).end(end).grain(ChronoUnit.MONTHS)
                    .build().tile();
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
            // [14:30, 17:30) by HOURS
            var tiles = new TemporalTiler.Builder<LocalDateTime>()
                    .start(LocalDateTime.of(2025, 3, 15, 14, 30))
                    .end(LocalDateTime.of(2025, 3, 15, 17, 30))
                    .grain(ChronoUnit.HOURS)
                    .build().tile();
            assertThat(tiles).hasSize(4);
            // Head partial: [14:30, 15:00)
            asserTile(tiles.getFirst())
                    .hasStart(LocalDateTime.of(2025, 3, 15, 14, 30))
                    .hasEnd(LocalDateTime.of(2025, 3, 15, 15, 0))
                    .isNotAligned();
            // Full hours: [15:00, 16:00), [16:00, 17:00)
            asserTile(tiles.get(1)).isAligned();
            asserTile(tiles.get(2)).isAligned();
            // Tail partial: [17:00, 17:30)
            asserTile(tiles.getLast())
                    .hasStart(LocalDateTime.of(2025, 3, 15, 17, 0))
                    .hasEnd(LocalDateTime.of(2025, 3, 15, 17, 30))
                    .isNotAligned();
        }

        @Test
        void _Days_PartialHeadAndTail() {
            // [Mar 15 10:00, Mar 18 14:00) by DAYS
            var tiles = new TemporalTiler.Builder<LocalDateTime>()
                    .start(LocalDateTime.of(2025, 3, 15, 10, 0))
                    .end(LocalDateTime.of(2025, 3, 18, 14, 0))
                    .grain(ChronoUnit.DAYS)
                    .build().tile();
            assertThat(tiles).hasSize(4);
            // Head partial: [Mar 15 10:00, Mar 16 00:00)
            asserTile(tiles.getFirst())
                    .hasStart(LocalDateTime.of(2025, 3, 15, 10, 0))
                    .hasEnd(LocalDateTime.of(2025, 3, 16, 0, 0))
                    .isNotAligned();
            // Full days: [Mar 16, Mar 17), [Mar 17, Mar 18)
            asserTile(tiles.get(1)).isAligned();
            asserTile(tiles.get(2)).isAligned();
            // Tail partial: [Mar 18 00:00, Mar 18 14:00)
            asserTile(tiles.getLast()).isNotAligned();
        }

        @Test
        void _Months_AllAligned() {
            // [Apr 1 00:00, Jul 1 00:00) by MONTHS
            var tiles = new TemporalTiler.Builder<LocalDateTime>()
                    .start(LocalDateTime.of(2025, 4, 1, 0, 0))
                    .end(LocalDateTime.of(2025, 7, 1, 0, 0))
                    .grain(ChronoUnit.MONTHS)
                    .build().tile();
            assertThat(tiles).hasSize(3);
            assertThat(tiles).allSatisfy(t -> asserTile(t).isAligned());
        }

        @Test
        void _Minutes_PartialHead() {
            // [14:15:30, 14:18:00) by MINUTES
            var tiles = new TemporalTiler.Builder<LocalDateTime>()
                    .start(LocalDateTime.of(2025, 3, 15, 14, 15, 30))
                    .end(LocalDateTime.of(2025, 3, 15, 14, 18, 0))
                    .grain(ChronoUnit.MINUTES)
                    .build().tile();
            assertThat(tiles).hasSize(3);
            // Head partial: [14:15:30, 14:16:00)
            asserTile(tiles.getFirst()).isNotAligned();
            // Full minutes: [14:16, 14:17), [14:17, 14:18)
            asserTile(tiles.get(1)).isAligned();
            asserTile(tiles.get(2)).isAligned();
        }
    }

    @Nested
    class OffsetDateTimeTest {

        @Test
        void _Hours_PartialHeadAndTail() {
            var offset = ZoneOffset.ofHours(9);
            var tiles = new TemporalTiler.Builder<OffsetDateTime>()
                    .start(OffsetDateTime.of(2025, 3, 15, 14, 30, 0, 0, offset))
                    .end(OffsetDateTime.of(2025, 3, 15, 17, 15, 0, 0, offset))
                    .grain(ChronoUnit.HOURS)
                    .build().tile();
            assertThat(tiles).hasSize(4);
            // Head partial: [14:30, 15:00)
            asserTile(tiles.getFirst())
                    .hasStart(OffsetDateTime.of(2025, 3, 15, 14, 30, 0, 0, offset))
                    .hasEnd(OffsetDateTime.of(2025, 3, 15, 15, 0, 0, 0, offset))
                    .isNotAligned();
            // Full hours: [15:00, 16:00), [16:00, 17:00)
            asserTile(tiles.get(1)).isAligned();
            asserTile(tiles.get(2)).isAligned();
            // Tail partial: [17:00, 17:15)
            asserTile(tiles.getLast()).isNotAligned();
        }

        @Test
        void _Days_AllAligned() {
            var offset = ZoneOffset.UTC;
            var tiles = new TemporalTiler.Builder<OffsetDateTime>()
                    .start(OffsetDateTime.of(2025, 3, 15, 0, 0, 0, 0, offset))
                    .end(OffsetDateTime.of(2025, 3, 18, 0, 0, 0, 0, offset))
                    .grain(ChronoUnit.DAYS)
                    .build().tile();
            assertThat(tiles).hasSize(3);
            assertThat(tiles).allSatisfy(t -> asserTile(t).isAligned());
        }

        @Test
        void _Months_PartialHeadAndTail() {
            var offset = ZoneOffset.ofHours(-5);
            var tiles = new TemporalTiler.Builder<OffsetDateTime>()
                    .start(OffsetDateTime.of(2025, 3, 15, 0, 0, 0, 0, offset))
                    .end(OffsetDateTime.of(2025, 6, 10, 0, 0, 0, 0, offset))
                    .grain(ChronoUnit.MONTHS)
                    .build().tile();
            assertThat(tiles).hasSize(4);
            asserTile(tiles.getFirst()).isNotAligned();
            asserTile(tiles.get(1)).isAligned();
            asserTile(tiles.get(2)).isAligned();
            asserTile(tiles.getLast()).isNotAligned();
        }
    }

    @Nested
    class ZonedDateTimeTest {

        @Test
        void _Hours_PartialHeadAndTail() {
            var zone = ZoneId.of("America/New_York");
            var tiles = new TemporalTiler.Builder<ZonedDateTime>()
                    .start(ZonedDateTime.of(2025, 3, 15, 14, 30, 0, 0, zone))
                    .end(ZonedDateTime.of(2025, 3, 15, 17, 45, 0, 0, zone))
                    .grain(ChronoUnit.HOURS)
                    .build().tile();
            assertThat(tiles).hasSize(4);
            // Head partial: [14:30, 15:00)
            asserTile(tiles.getFirst()).isNotAligned();
            // Full hours: [15:00, 16:00), [16:00, 17:00)
            asserTile(tiles.get(1)).isAligned();
            asserTile(tiles.get(2)).isAligned();
            // Tail partial: [17:00, 17:45)
            asserTile(tiles.getLast()).isNotAligned();
        }

        @Test
        void _Days_PartialHead() {
            var zone = ZoneId.of("Europe/London");
            var tiles = new TemporalTiler.Builder<ZonedDateTime>()
                    .start(ZonedDateTime.of(2025, 3, 15, 10, 0, 0, 0, zone))
                    .end(ZonedDateTime.of(2025, 3, 18, 0, 0, 0, 0, zone))
                    .grain(ChronoUnit.DAYS)
                    .build().tile();
            assertThat(tiles).hasSize(3);
            // Head partial: [Mar 15 10:00, Mar 16 00:00)
            asserTile(tiles.getFirst()).isNotAligned();
            // Full days: [Mar 16, Mar 17), [Mar 17, Mar 18)
            asserTile(tiles.get(1)).isAligned();
            asserTile(tiles.get(2)).isAligned();
        }

        @Test
        void _Months_AllAligned() {
            var zone = ZoneId.of("Asia/Tokyo");
            var tiles = new TemporalTiler.Builder<ZonedDateTime>()
                    .start(ZonedDateTime.of(2025, 4, 1, 0, 0, 0, 0, zone))
                    .end(ZonedDateTime.of(2025, 7, 1, 0, 0, 0, 0, zone))
                    .grain(ChronoUnit.MONTHS)
                    .build().tile();
            assertThat(tiles).hasSize(3);
            assertThat(tiles).allSatisfy(t -> asserTile(t).isAligned());
        }

        @Test
        void _EmptyRange() {
            var zone = ZoneId.of("UTC");
            var zdt = ZonedDateTime.of(2025, 3, 15, 12, 0, 0, 0, zone);
            var tiles = new TemporalTiler.Builder<ZonedDateTime>()
                    .start(zdt).end(zdt).grain(ChronoUnit.HOURS)
                    .build().tile();
            assertThat(tiles).isEmpty();
        }
    }

    @Test
    void _Months_PartialHeadAndTail() {
        // [Mar 15, Jun 10) by MONTHS
        var tiles = new TemporalTiler.Builder<LocalDate>()
                .start(LocalDate.of(2025, 3, 15)).end(LocalDate.of(2025, 6, 10)).grain(ChronoUnit.MONTHS)
                .build().tile();
        assertThat(tiles).hasSize(4);
        // Head partial: [Mar 15, Apr 1)
        asserTile(tiles.getFirst())
                .hasStart(LocalDate.of(2025, 3, 15))
                .hasEnd(LocalDate.of(2025, 4, 1))
                .isNotAligned();
        // Full month: [Apr 1, May 1)
        asserTile(tiles.get(1))
                .hasStart(LocalDate.of(2025, 4, 1))
                .hasEnd(LocalDate.of(2025, 5, 1))
                .isAligned();
        // Full month: [May 1, Jun 1)
        asserTile(tiles.get(2))
                .hasStart(LocalDate.of(2025, 5, 1))
                .hasEnd(LocalDate.of(2025, 6, 1))
                .isAligned();
        // Tail partial: [Jun 1, Jun 10)
        asserTile(tiles.getLast())
                .hasStart(LocalDate.of(2025, 6, 1))
                .hasEnd(LocalDate.of(2025, 6, 10))
                .isNotAligned();
    }

    @Test
    void _Months_AlignedStartAndEnd() {
        // [Apr 1, Jul 1) by MONTHS — perfectly aligned
        var tiles = new TemporalTiler.Builder<LocalDate>()
                .start(LocalDate.of(2025, 4, 1)).end(LocalDate.of(2025, 7, 1)).grain(ChronoUnit.MONTHS)
                .build().tile();
        assertThat(tiles).hasSize(3);
        assertThat(tiles).allSatisfy(t -> asserTile(t).isAligned());
    }

    @Test
    void _Years_PartialHeadAndTail() {
        // [Mar 15, Jun 10 next year) by YEARS
        var tiles = new TemporalTiler.Builder<LocalDate>()
                .start(LocalDate.of(2025, 3, 15)).end(LocalDate.of(2027, 6, 10)).grain(ChronoUnit.YEARS)
                .build().tile();
        assertThat(tiles).hasSize(3);
        // Head partial: [2025-03-15, 2026-01-01)
        asserTile(tiles.getFirst())
                .hasStart(LocalDate.of(2025, 3, 15))
                .hasEnd(LocalDate.of(2026, 1, 1))
                .isNotAligned();
        // Full year: [2026-01-01, 2027-01-01)
        asserTile(tiles.get(1))
                .hasStart(LocalDate.of(2026, 1, 1))
                .hasEnd(LocalDate.of(2027, 1, 1))
                .isAligned();
        // Tail partial: [2027-01-01, 2027-06-10)
        asserTile(tiles.getLast())
                .hasStart(LocalDate.of(2027, 1, 1))
                .hasEnd(LocalDate.of(2027, 6, 10))
                .isNotAligned();
    }

    @Test
    void _Days_AllAligned() {
        // [Mar 15, Mar 18) by DAYS
        var tiles = new TemporalTiler.Builder<LocalDate>()
                .start(LocalDate.of(2025, 3, 15)).end(LocalDate.of(2025, 3, 18)).grain(ChronoUnit.DAYS)
                .build().tile();
        assertThat(tiles).hasSize(3);
        assertThat(tiles).allSatisfy(t -> asserTile(t).isAligned());
        asserTile(tiles.getFirst())
                .hasStart(LocalDate.of(2025, 3, 15))
                .hasEnd(LocalDate.of(2025, 3, 16));
    }

    @Test
    void _Hours_PartialHead() {
        // [14:30, 17:00) by HOURS
        var tiles = new TemporalTiler.Builder<LocalDateTime>()
                .start(LocalDateTime.of(2025, 3, 15, 14, 30))
                .end(LocalDateTime.of(2025, 3, 15, 17, 0))
                .grain(ChronoUnit.HOURS)
                .build().tile();
        assertThat(tiles).hasSize(3);
        // Head partial: [14:30, 15:00)
        asserTile(tiles.getFirst())
                .hasStart(LocalDateTime.of(2025, 3, 15, 14, 30))
                .hasEnd(LocalDateTime.of(2025, 3, 15, 15, 0))
                .isNotAligned();
        // Full hour: [15:00, 16:00)
        asserTile(tiles.get(1)).isAligned();
        // Full hour: [16:00, 17:00)
        asserTile(tiles.get(2)).isAligned();
    }

    @Test
    void _EmptyRange() {
        var tiles = new TemporalTiler.Builder<LocalDate>()
                .start(LocalDate.of(2025, 3, 15)).end(LocalDate.of(2025, 3, 15)).grain(ChronoUnit.MONTHS)
                .build().tile();
        assertThat(tiles).isEmpty();
    }

    @Test
    void _SmallerThanGrain() {
        // [Mar 15, Mar 20) by MONTHS — smaller than one month
        var tiles = new TemporalTiler.Builder<LocalDate>()
                .start(LocalDate.of(2025, 3, 15)).end(LocalDate.of(2025, 3, 20)).grain(ChronoUnit.MONTHS)
                .build().tile();
        assertThat(tiles).hasSize(1);
        asserTile(tiles.getFirst())
                .hasStart(LocalDate.of(2025, 3, 15))
                .hasEnd(LocalDate.of(2025, 3, 20))
                .isNotAligned();
    }

    @Test
    void _Hierarchical_UserDriven() {
        // Year → Month chaining
        var start = LocalDate.of(2025, 3, 15);
        var end = LocalDate.of(2027, 6, 10);

        var yearTiles = new TemporalTiler.Builder<LocalDate>()
                .start(start).end(end).grain(ChronoUnit.YEARS)
                .build().tile();
        assertThat(yearTiles).hasSize(3); // head partial, full 2026, tail partial

        // Decompose the head partial year into months
        var headMonths = new TemporalTiler.Builder<LocalDate>()
                .start(yearTiles.getFirst().getStart())
                .end(yearTiles.getFirst().getEnd())
                .grain(ChronoUnit.MONTHS)
                .build().tile();

        // Head partial year [2025-03-15, 2026-01-01) should have:
        // partial March, full Apr-Dec = 1 partial + 9 full = 10 tiles
        assertThat(headMonths).hasSize(10);
        asserTile(headMonths.getFirst()).isNotAligned(); // partial March
        asserTile(headMonths.get(1)).isAligned();       // full April
    }

    @Test
    void _GapFree() {
        // Verify tiles cover the entire range with no gaps
        var start = LocalDate.of(2025, 3, 15);
        var end = LocalDate.of(2025, 8, 22);
        var tiles = new TemporalTiler.Builder<LocalDate>()
                .start(start).end(end).grain(ChronoUnit.MONTHS)
                .build().tile();
        assertThat(tiles.getFirst().getStart()).isEqualTo(start);
        assertThat(tiles.getLast().getEnd()).isEqualTo(end);
        for (int i = 0; i < tiles.size() - 1; i++) {
            assertThat(tiles.get(i).getEnd()).isEqualTo(tiles.get(i + 1).getStart());
        }
    }
}
