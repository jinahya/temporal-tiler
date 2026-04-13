package com.github.jinahya.time.temporal.tile;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/**
 * Tests for {@link TemporalTiler}, demonstrating {@link YearMonth}-based tiling by {@link ChronoUnit#MONTHS} and
 * hierarchical tiling from {@link YearMonth} down to {@link ChronoUnit#DAYS} via conversion to
 * {@link java.time.LocalDate}.
 *
 * @see TemporalTiler
 */
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Slf4j
class TemporalTilerTest {

    @Test
    void __YearMonth_ByMonths() {
        final var start = YearMonth.now();
        final var end = start.plusYears(1L);
        final var tiles = TemporalTiler.tile(start, end, ChronoUnit.MONTHS);
        tiles.forEach(mt -> {
            log.debug("\tmonth: {}", mt);
        });
        TemporalTiles_TestUtils.verify(ChronoUnit.MONTHS, tiles);
    }

    @Test
    void __YearMonth_ByMonths_ThenDays() {
        final var start = YearMonth.now();
        final var end = start.plusYears(1L);
        TemporalTiler.tile(start, end, ChronoUnit.MONTHS).forEach(mt -> {
            log.debug("\tmonth: {}", mt);
            // convert to LocalDate for day-level tiling
            final var dayStart = mt.start().atDay(1);
            final var dayEnd = mt.end().atDay(1);
            TemporalTiler.tile(dayStart, dayEnd, ChronoUnit.DAYS).forEach(dt -> {
                log.debug("\t\tday: {}", dt);
            });
        });
    }
}
