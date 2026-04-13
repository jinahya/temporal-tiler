package com.github.jinahya.time.temporal.tile;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.stream.Stream;

import static com.github.jinahya.time.temporal.tile.TemporalTileAssert.assertTile;
import static com.github.jinahya.time.temporal.tile.TemporalTileListAssert.assertTiles;
import static com.github.jinahya.time.temporal.tile._TestConstants.SUPPORTED_GRAINS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * Tests all supported {@link Temporal} type / {@link ChronoUnit} grain combinations via {@code @TestFactory},
 * covering the four standard test patterns: all-aligned, empty range, partial head-and-tail, and smaller-than-grain.
 *
 * @see _TestConstants#SUPPORTED_GRAINS
 * @see TemporalTiler
 */
@Slf4j
class TemporalTiler_AllCombinations_Test {

    // -----------------------------------------------------------------------------------------------------------------

    @TestFactory
    Stream<DynamicTest> _AllAligned() {
        return combinations()
                .map(c -> dynamicTest(
                        c.type().getSimpleName() + " / " + c.grain() + " / AllAligned",
                        () -> doAllAligned(c.type(), c.grain())));
    }

    @TestFactory
    Stream<DynamicTest> _EmptyRange() {
        return combinations()
                .map(c -> dynamicTest(
                        c.type().getSimpleName() + " / " + c.grain() + " / EmptyRange",
                        () -> doEmptyRange(c.type(), c.grain())));
    }

    @TestFactory
    Stream<DynamicTest> _PartialHeadAndTail() {
        return combinations()
                .filter(c -> canHavePartialHeadAndTail(c.type(), c.grain()))
                .map(c -> dynamicTest(
                        c.type().getSimpleName() + " / " + c.grain() + " / PartialHeadAndTail",
                        () -> doPartialHeadAndTail(c.type(), c.grain())));
    }

    @TestFactory
    Stream<DynamicTest> _SmallerThanGrain() {
        return combinations()
                .filter(c -> canBeUnaligned(c.type(), c.grain()))
                .map(c -> dynamicTest(
                        c.type().getSimpleName() + " / " + c.grain() + " / SmallerThanGrain",
                        () -> doSmallerThanGrain(c.type(), c.grain())));
    }

    // ------------------------------------------------------------------------------------------------ test method impls

    /**
     * Tiles a grain-aligned range and verifies all tiles are aligned.
     */
    @SuppressWarnings("unchecked")
    private static <T extends Temporal & Comparable<? super T>>
    void doAllAligned(final Class<?> type, final ChronoUnit grain) {
        final var start = (T) alignedStart(type, grain);
        final var count = grainCount(type, grain);
        final var end = (T) start.plus(count, grain);
        final var tiles = TemporalTiler.tile(start, end, grain);
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).hasSize(count);
        assertTiles(tiles).isAllAligned();
    }

    /**
     * Tiles a range where {@code start == end} and verifies the result is empty.
     */
    @SuppressWarnings("unchecked")
    private static <T extends Temporal & Comparable<? super T>>
    void doEmptyRange(final Class<?> type, final ChronoUnit grain) {
        final var start = (T) alignedStart(type, grain);
        final var tiles = TemporalTiler.tile(start, start, grain);
        assertThat(tiles).isEmpty();
    }

    /**
     * Tiles a range starting and ending off-boundary, verifying partial head/tail and aligned interior.
     */
    @SuppressWarnings("unchecked")
    private static <T extends Temporal & Comparable<? super T>>
    void doPartialHeadAndTail(final Class<?> type, final ChronoUnit grain) {
        final var aligned = (T) alignedStart(type, grain);
        final var sub = subGrainUnit(grain);
        final var start = (T) aligned.plus(1, sub);
        final var count = grainCount(type, grain);
        final var end = (T) start.plus(count, grain);
        final var tiles = TemporalTiler.tile(start, end, grain);
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles.size()).isGreaterThanOrEqualTo(count + 1);
        assertTile(tiles.getFirst()).isNotAligned();
        assertTile(tiles.getLast()).isNotAligned();
        assertTiles(tiles).hasAlignedInterior();
    }

    /**
     * Tiles a range smaller than one grain unit, verifying a single partial tile.
     */
    @SuppressWarnings("unchecked")
    private static <T extends Temporal & Comparable<? super T>>
    void doSmallerThanGrain(final Class<?> type, final ChronoUnit grain) {
        final var aligned = (T) alignedStart(type, grain);
        final var sub = subGrainUnit(grain);
        final var start = (T) aligned.plus(1, sub);
        final var end = (T) aligned.plus(1, grain); // next grain boundary
        final var tiles = TemporalTiler.tile(start, end, grain);
        TemporalTiles_TestUtils.verify(grain, tiles);
        assertThat(tiles).hasSize(1);
        assertTile(tiles.getFirst()).isNotAligned();
    }

    // ---------------------------------------------------------------------------------------------------- combinations

    /**
     * A (temporal type, grain) pair for parameterized test generation.
     */
    private record Combination(Class<? extends Temporal> type, ChronoUnit grain) {
    }

    /**
     * Returns a stream of all supported (type, grain) combinations from {@link _TestConstants#SUPPORTED_GRAINS}.
     */
    private static Stream<Combination> combinations() {
        return SUPPORTED_GRAINS.entrySet().stream()
                .flatMap(e -> e.getValue().stream()
                        .map(grain -> new Combination(e.getKey(), grain)));
    }

    // -------------------------------------------------------------------------------------------------- aligned starts

    /**
     * Returns a grain-boundary-aligned start value for the given temporal type and grain.
     */
    @SuppressWarnings("unchecked")
    private static <T extends Temporal> T alignedStart(final Class<?> type, final ChronoUnit grain) {
        if (type == LocalTime.class) {
            return (T) LocalTime.MIDNIGHT;
        }
        if (type == LocalDate.class) {
            return (T) alignedLocalDate(grain);
        }
        if (type == LocalDateTime.class) {
            return (T) (grain.isTimeBased()
                    ? LocalDateTime.of(2000, 1, 1, 0, 0)
                    : LocalDateTime.of(alignedLocalDate(grain), LocalTime.MIDNIGHT));
        }
        if (type == OffsetDateTime.class) {
            return (T) (grain.isTimeBased()
                    ? OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
                    : OffsetDateTime.of(alignedLocalDate(grain), LocalTime.MIDNIGHT, ZoneOffset.UTC));
        }
        if (type == ZonedDateTime.class) {
            return (T) (grain.isTimeBased()
                    ? ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
                    : ZonedDateTime.of(alignedLocalDate(grain), LocalTime.MIDNIGHT, ZoneId.of("UTC")));
        }
        if (type == YearMonth.class) {
            return (T) switch (grain) {
                case MONTHS, YEARS -> YearMonth.of(2025, 1);
                case DECADES -> YearMonth.of(2020, 1);
                case CENTURIES, MILLENNIA -> YearMonth.of(2000, 1);
                default -> throw new IllegalArgumentException("Unsupported grain for YearMonth: " + grain);
            };
        }
        if (type == Year.class) {
            return (T) switch (grain) {
                case YEARS -> Year.of(2025);
                case DECADES -> Year.of(2020);
                case CENTURIES, MILLENNIA -> Year.of(2000);
                default -> throw new IllegalArgumentException("Unsupported grain for Year: " + grain);
            };
        }
        throw new IllegalArgumentException("Unsupported type: " + type);
    }

    /**
     * Returns a {@link LocalDate} that is aligned to the specified grain boundary.
     */
    private static LocalDate alignedLocalDate(final ChronoUnit grain) {
        return switch (grain) {
            case DAYS -> LocalDate.of(2025, 6, 15);
            case WEEKS -> LocalDate.of(2024, 1, 1); // Monday
            case MONTHS -> LocalDate.of(2025, 1, 1);
            case YEARS -> LocalDate.of(2025, 1, 1);
            case DECADES -> LocalDate.of(2020, 1, 1);
            case CENTURIES, MILLENNIA -> LocalDate.of(2000, 1, 1);
            default -> throw new IllegalArgumentException("Unsupported grain for LocalDate: " + grain);
        };
    }

    // ----------------------------------------------------------------------------------------------------- sub-grains

    /**
     * Returns the next-finer {@link ChronoUnit} below the given grain, used to create an unaligned offset.
     */
    private static ChronoUnit subGrainUnit(final ChronoUnit grain) {
        return switch (grain) {
            case MICROS -> ChronoUnit.NANOS;
            case MILLIS -> ChronoUnit.MICROS;
            case SECONDS -> ChronoUnit.MILLIS;
            case MINUTES -> ChronoUnit.SECONDS;
            case HOURS -> ChronoUnit.MINUTES;
            case HALF_DAYS -> ChronoUnit.HOURS;
            case DAYS -> ChronoUnit.HOURS;
            case WEEKS -> ChronoUnit.DAYS;
            case MONTHS -> ChronoUnit.DAYS;
            case YEARS -> ChronoUnit.MONTHS;
            case DECADES, CENTURIES, MILLENNIA -> ChronoUnit.YEARS;
            default -> throw new IllegalArgumentException("No sub-grain for: " + grain);
        };
    }

    /**
     * Returns {@code true} if values of the given type can be unaligned to the given grain. Returns {@code false} when
     * the type's precision matches the grain (every value is inherently aligned).
     */
    private static boolean canBeUnaligned(final Class<?> type, final ChronoUnit grain) {
        // NANOS is the finest possible grain — every value is aligned
        if (grain == ChronoUnit.NANOS) return false;
        if (type == LocalDate.class && grain == ChronoUnit.DAYS) return false;
        if (type == YearMonth.class && grain == ChronoUnit.MONTHS) return false;
        if (type == Year.class && grain == ChronoUnit.YEARS) return false;
        return true;
    }

    /**
     * Returns {@code true} if partial-head-and-tail tests are feasible for the given combination. Returns {@code false}
     * when values cannot be unaligned, or when adding grains would cause wrapping (e.g., {@link LocalTime} with
     * {@link ChronoUnit#HALF_DAYS}).
     */
    private static boolean canHavePartialHeadAndTail(final Class<?> type, final ChronoUnit grain) {
        if (!canBeUnaligned(type, grain)) return false;
        // LocalTime with HALF_DAYS: adding grains wraps past 24 hours
        if (type == LocalTime.class && grain == ChronoUnit.HALF_DAYS) return false;
        return true;
    }

    /**
     * Returns the number of grains to use for aligned/partial tests, capped to avoid wrapping on {@link LocalTime}.
     */
    private static int grainCount(final Class<?> type, final ChronoUnit grain) {
        if (type == LocalTime.class && grain == ChronoUnit.HALF_DAYS) {
            return 1;
        }
        return 3;
    }
}
