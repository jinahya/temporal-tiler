package com.github.jinahya.time.temporal.tile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Shared constants used across test classes.
 */
final class _TestConstants {

    /**
     * The number of days in a week, derived from {@link ChronoField#DAY_OF_WEEK}.
     */
    static final long NUMBER_OF_DAYS_OF_A_WEEK = ChronoField.DAY_OF_WEEK.range().getMaximum();

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Supported {@link ChronoUnit} grains for each {@link Temporal} type.
     */
    static final Map<Class<? extends Temporal>, Set<ChronoUnit>> SUPPORTED_GRAINS = Map.of(
            LocalTime.class, EnumSet.of(
                    ChronoUnit.NANOS, ChronoUnit.MICROS, ChronoUnit.MILLIS,
                    ChronoUnit.SECONDS, ChronoUnit.MINUTES, ChronoUnit.HOURS,
                    ChronoUnit.HALF_DAYS
            ),
            LocalDate.class, EnumSet.of(
                    ChronoUnit.DAYS, ChronoUnit.WEEKS, ChronoUnit.MONTHS,
                    ChronoUnit.YEARS, ChronoUnit.DECADES, ChronoUnit.CENTURIES,
                    ChronoUnit.MILLENNIA
            ),
            LocalDateTime.class, EnumSet.of(
                    ChronoUnit.NANOS, ChronoUnit.MICROS, ChronoUnit.MILLIS,
                    ChronoUnit.SECONDS, ChronoUnit.MINUTES, ChronoUnit.HOURS,
                    ChronoUnit.HALF_DAYS, ChronoUnit.DAYS, ChronoUnit.WEEKS,
                    ChronoUnit.MONTHS, ChronoUnit.YEARS, ChronoUnit.DECADES,
                    ChronoUnit.CENTURIES, ChronoUnit.MILLENNIA
            ),
            OffsetDateTime.class, EnumSet.of(
                    ChronoUnit.NANOS, ChronoUnit.MICROS, ChronoUnit.MILLIS,
                    ChronoUnit.SECONDS, ChronoUnit.MINUTES, ChronoUnit.HOURS,
                    ChronoUnit.HALF_DAYS, ChronoUnit.DAYS, ChronoUnit.WEEKS,
                    ChronoUnit.MONTHS, ChronoUnit.YEARS, ChronoUnit.DECADES,
                    ChronoUnit.CENTURIES, ChronoUnit.MILLENNIA
            ),
            ZonedDateTime.class, EnumSet.of(
                    ChronoUnit.NANOS, ChronoUnit.MICROS, ChronoUnit.MILLIS,
                    ChronoUnit.SECONDS, ChronoUnit.MINUTES, ChronoUnit.HOURS,
                    ChronoUnit.HALF_DAYS, ChronoUnit.DAYS, ChronoUnit.WEEKS,
                    ChronoUnit.MONTHS, ChronoUnit.YEARS, ChronoUnit.DECADES,
                    ChronoUnit.CENTURIES, ChronoUnit.MILLENNIA
            ),
            YearMonth.class, EnumSet.of(
                    ChronoUnit.MONTHS, ChronoUnit.YEARS, ChronoUnit.DECADES,
                    ChronoUnit.CENTURIES, ChronoUnit.MILLENNIA
            ),
            Year.class, EnumSet.of(
                    ChronoUnit.YEARS, ChronoUnit.DECADES, ChronoUnit.CENTURIES,
                    ChronoUnit.MILLENNIA
            )
    );

    // -----------------------------------------------------------------------------------------------------------------

    private _TestConstants() {
        throw new AssertionError("instantiation is not allowed");
    }
}
