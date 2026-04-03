package com.github.jinahya.time.temporal;

import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemporalTilerUtilsTest {

    @Test
    void findPath_YearsToDays() {
        var path = TemporalTilerUtils.findPath(ChronoUnit.YEARS, ChronoUnit.DAYS);
        assertTrue(path.isPresent());
        assertEquals(List.of(ChronoUnit.YEARS, ChronoUnit.MONTHS, ChronoUnit.DAYS), path.get());
    }

    @Test
    void findPath_YearsToHours() {
        var path = TemporalTilerUtils.findPath(ChronoUnit.YEARS, ChronoUnit.HOURS);
        assertTrue(path.isPresent());
        assertEquals(
                List.of(ChronoUnit.YEARS, ChronoUnit.MONTHS, ChronoUnit.DAYS,
                        ChronoUnit.HALF_DAYS, ChronoUnit.HOURS),
                path.get()
        );
    }

    @Test
    void findPath_WeeksToDays() {
        var path = TemporalTilerUtils.findPath(ChronoUnit.WEEKS, ChronoUnit.DAYS);
        assertTrue(path.isPresent());
        assertEquals(List.of(ChronoUnit.WEEKS, ChronoUnit.DAYS), path.get());
    }

    @Test
    void findPath_WeeksToHours() {
        var path = TemporalTilerUtils.findPath(ChronoUnit.WEEKS, ChronoUnit.HOURS);
        assertTrue(path.isPresent());
        assertEquals(
                List.of(ChronoUnit.WEEKS, ChronoUnit.DAYS, ChronoUnit.HALF_DAYS, ChronoUnit.HOURS),
                path.get()
        );
    }

    @Test
    void findPath_DaysToHalfDays() {
        var path = TemporalTilerUtils.findPath(ChronoUnit.DAYS, ChronoUnit.HALF_DAYS);
        assertTrue(path.isPresent());
        assertEquals(List.of(ChronoUnit.DAYS, ChronoUnit.HALF_DAYS), path.get());
    }

    @Test
    void findPath_SameUnit() {
        var path = TemporalTilerUtils.findPath(ChronoUnit.DAYS, ChronoUnit.DAYS);
        assertTrue(path.isPresent());
        assertEquals(List.of(ChronoUnit.DAYS), path.get());
    }

    @Test
    void findPath_MillenniaToNanos() {
        var path = TemporalTilerUtils.findPath(ChronoUnit.MILLENNIA, ChronoUnit.NANOS);
        assertTrue(path.isPresent());
        assertEquals(TemporalTilerConstants.MAIN_CHAIN, path.get());
    }

    @Test
    void findPath_HoursToMinutes() {
        var path = TemporalTilerUtils.findPath(ChronoUnit.HOURS, ChronoUnit.MINUTES);
        assertTrue(path.isPresent());
        assertEquals(List.of(ChronoUnit.HOURS, ChronoUnit.MINUTES), path.get());
    }

    // --- Invalid paths ---

    @Test
    void findPath_MonthsToWeeks_NoPath() {
        var path = TemporalTilerUtils.findPath(ChronoUnit.MONTHS, ChronoUnit.WEEKS);
        assertFalse(path.isPresent());
    }

    @Test
    void findPath_YearsToWeeks_NoPath() {
        var path = TemporalTilerUtils.findPath(ChronoUnit.YEARS, ChronoUnit.WEEKS);
        assertFalse(path.isPresent());
    }

    @Test
    void findPath_Reversed_DaysToYears_NoPath() {
        var path = TemporalTilerUtils.findPath(ChronoUnit.DAYS, ChronoUnit.YEARS);
        assertFalse(path.isPresent());
    }

    @Test
    void findPath_NanosToMillis_NoPath() {
        var path = TemporalTilerUtils.findPath(ChronoUnit.NANOS, ChronoUnit.MILLIS);
        assertFalse(path.isPresent());
    }

    @Test
    void findPath_Forever_NoPath() {
        var path = TemporalTilerUtils.findPath(ChronoUnit.FOREVER, ChronoUnit.DAYS);
        assertFalse(path.isPresent());
    }

    @Test
    void findPath_Eras_NoPath() {
        var path = TemporalTilerUtils.findPath(ChronoUnit.ERAS, ChronoUnit.DAYS);
        assertFalse(path.isPresent());
    }

    // --- directChildren ---

    @Test
    void directChildren_Years() {
        assertEquals(List.of(ChronoUnit.MONTHS), TemporalTilerUtils.directChildren(ChronoUnit.YEARS));
    }

    @Test
    void directChildren_Months() {
        assertEquals(List.of(ChronoUnit.DAYS), TemporalTilerUtils.directChildren(ChronoUnit.MONTHS));
    }

    @Test
    void directChildren_Days() {
        assertEquals(List.of(ChronoUnit.HALF_DAYS), TemporalTilerUtils.directChildren(ChronoUnit.DAYS));
    }

    @Test
    void directChildren_Weeks() {
        assertEquals(List.of(ChronoUnit.DAYS), TemporalTilerUtils.directChildren(ChronoUnit.WEEKS));
    }

    @Test
    void directChildren_Nanos_Empty() {
        assertEquals(List.of(), TemporalTilerUtils.directChildren(ChronoUnit.NANOS));
    }

    @Test
    void directChildren_Forever_Empty() {
        assertEquals(List.of(), TemporalTilerUtils.directChildren(ChronoUnit.FOREVER));
    }

    // --- directParents ---

    @Test
    void directParents_Days_HasTwo() {
        assertEquals(
                List.of(ChronoUnit.MONTHS, ChronoUnit.WEEKS),
                TemporalTilerUtils.directParents(ChronoUnit.DAYS)
        );
    }

    @Test
    void directParents_Months() {
        assertEquals(List.of(ChronoUnit.YEARS), TemporalTilerUtils.directParents(ChronoUnit.MONTHS));
    }

    @Test
    void directParents_Weeks_Empty() {
        assertEquals(List.of(), TemporalTilerUtils.directParents(ChronoUnit.WEEKS));
    }

    @Test
    void directParents_Millennia_Empty() {
        assertEquals(List.of(), TemporalTilerUtils.directParents(ChronoUnit.MILLENNIA));
    }

    // --- subPath ---

    @Test
    void subPath_Years() {
        var path = TemporalTilerUtils.subPath(ChronoUnit.YEARS);
        assertEquals(ChronoUnit.YEARS, path.getFirst());
        assertEquals(ChronoUnit.NANOS, path.getLast());
        assertTrue(path.contains(ChronoUnit.MONTHS));
        assertTrue(path.contains(ChronoUnit.DAYS));
        assertFalse(path.contains(ChronoUnit.WEEKS));
    }

    @Test
    void subPath_Weeks() {
        var path = TemporalTilerUtils.subPath(ChronoUnit.WEEKS);
        assertEquals(ChronoUnit.WEEKS, path.getFirst());
        assertEquals(ChronoUnit.NANOS, path.getLast());
        assertTrue(path.contains(ChronoUnit.DAYS));
        assertFalse(path.contains(ChronoUnit.MONTHS));
    }

    @Test
    void subPath_Nanos() {
        assertEquals(List.of(ChronoUnit.NANOS), TemporalTilerUtils.subPath(ChronoUnit.NANOS));
    }

    @Test
    void subPath_Eras_Empty() {
        assertEquals(List.of(), TemporalTilerUtils.subPath(ChronoUnit.ERAS));
    }

    // --- hasPath convenience ---

    @Test
    void hasPath_ValidPaths() {
        assertTrue(TemporalTilerUtils.hasPath(ChronoUnit.YEARS, ChronoUnit.DAYS));
        assertTrue(TemporalTilerUtils.hasPath(ChronoUnit.WEEKS, ChronoUnit.DAYS));
        assertTrue(TemporalTilerUtils.hasPath(ChronoUnit.DAYS, ChronoUnit.HOURS));
        assertTrue(TemporalTilerUtils.hasPath(ChronoUnit.MONTHS, ChronoUnit.NANOS));
    }

    @Test
    void hasPath_InvalidPaths() {
        assertFalse(TemporalTilerUtils.hasPath(ChronoUnit.MONTHS, ChronoUnit.WEEKS));
        assertFalse(TemporalTilerUtils.hasPath(ChronoUnit.HOURS, ChronoUnit.DAYS));
        assertFalse(TemporalTilerUtils.hasPath(ChronoUnit.FOREVER, ChronoUnit.YEARS));
    }
}
