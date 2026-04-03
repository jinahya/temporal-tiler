package com.github.jinahya.time.temporal;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Utility methods for navigating the {@link ChronoUnit} decomposition graph.
 */
final class TemporalTilerUtils {

    /**
     * Returns the direct children of the given {@link ChronoUnit} in the decomposition graph.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code directChildren(YEARS)} → {@code [MONTHS]}</li>
     *   <li>{@code directChildren(MONTHS)} → {@code [DAYS]}</li>
     *   <li>{@code directChildren(DAYS)} → {@code [HALF_DAYS]}</li>
     *   <li>{@code directChildren(WEEKS)} → {@code [DAYS]}</li>
     *   <li>{@code directChildren(NANOS)} → {@code []}</li>
     * </ul>
     *
     * @param unit the unit to query
     * @return immutable list of direct children (may be empty)
     */
    static List<ChronoUnit> directChildren(final ChronoUnit unit) {
        Objects.requireNonNull(unit, "unit is null");
        return TemporalTilerConstants.CHILDREN.getOrDefault(unit, List.of());
    }

    /**
     * Returns the direct parents of the given {@link ChronoUnit} in the decomposition graph.
     *
     * <p>Most units have exactly one parent. {@link ChronoUnit#DAYS} has two:
     * {@code [MONTHS, WEEKS]}.
     *
     * @param unit the unit to query
     * @return immutable list of direct parents (may be empty)
     */
    static List<ChronoUnit> directParents(final ChronoUnit unit) {
        Objects.requireNonNull(unit, "unit is null");
        return TemporalTilerConstants.PARENTS.getOrDefault(unit, List.of());
    }

    /**
     * Returns the full sub-path reachable from the given {@link ChronoUnit} down to the deepest unit
     * ({@link ChronoUnit#NANOS}).
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code subPath(YEARS)} → {@code [YEARS, MONTHS, DAYS, HALF_DAYS, ..., NANOS]}</li>
     *   <li>{@code subPath(WEEKS)} → {@code [WEEKS, DAYS, HALF_DAYS, HOURS, ..., NANOS]}</li>
     *   <li>{@code subPath(NANOS)} → {@code [NANOS]}</li>
     *   <li>{@code subPath(ERAS)} → {@code []} (not in the graph)</li>
     * </ul>
     *
     * @param unit the starting unit
     * @return immutable list of the full downward path (may be empty if unit is not in the graph)
     */
    static List<ChronoUnit> subPath(final ChronoUnit unit) {
        Objects.requireNonNull(unit, "unit is null");
        return TemporalTilerConstants.SUB_PATHS.getOrDefault(unit, List.of());
    }

    /**
     * Finds the decomposition path from {@code ceiling} down to {@code floor}, following the decomposition graph
     * defined in {@link TemporalTilerConstants}.
     *
     * <p>Returns a list of {@link ChronoUnit} values from ceiling (inclusive) to
     * floor (inclusive) representing each grain level in the hierarchy.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code findPath(YEARS, DAYS)} → {@code [YEARS, MONTHS, DAYS]}</li>
     *   <li>{@code findPath(YEARS, HOURS)} → {@code [YEARS, MONTHS, DAYS, HALF_DAYS, HOURS]}</li>
     *   <li>{@code findPath(WEEKS, DAYS)} → {@code [WEEKS, DAYS]}</li>
     *   <li>{@code findPath(MONTHS, WEEKS)} → {@code empty} (no valid path)</li>
     *   <li>{@code findPath(DAYS, DAYS)} → {@code [DAYS]} (single-grain)</li>
     * </ul>
     *
     * @param ceiling the widest grain (start of path)
     * @param floor   the narrowest grain (end of path)
     * @return the path from ceiling to floor, or empty if no valid path exists
     */
    static Optional<List<ChronoUnit>> findPath(final ChronoUnit ceiling, final ChronoUnit floor) {
        Objects.requireNonNull(ceiling, "ceiling is null");
        Objects.requireNonNull(floor, "floor is null");
        var fullPath = TemporalTilerConstants.SUB_PATHS.getOrDefault(ceiling, List.of());
        var floorIndex = fullPath.indexOf(floor);
        if (floorIndex < 0) {
            return Optional.empty();
        }
        return Optional.of(fullPath.subList(0, floorIndex + 1));
    }

    /**
     * Returns {@code true} if a valid decomposition path exists from {@code ceiling} down to {@code floor}.
     *
     * @param ceiling the widest grain
     * @param floor   the narrowest grain
     * @return true if a path exists
     */
    static boolean hasPath(final ChronoUnit ceiling, final ChronoUnit floor) {
        return findPath(ceiling, floor).isPresent();
    }

    /**
     * Validates a user-specified grain sequence (from widest to narrowest).
     *
     * <p>Each consecutive pair {@code (grains[i], grains[i+1])} must satisfy:
     * <ol>
     *   <li>{@code grains[i+1]} is reachable from {@code grains[i]} in the decomposition graph
     *       (i.e., a boundary-aligned span of the wider unit always contains a whole number of
     *       the narrower unit)</li>
     *   <li>{@code grains[i]} is strictly wider than {@code grains[i+1]}</li>
     * </ol>
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code [YEARS, MONTHS, DAYS]} → valid (each hop reachable)</li>
     *   <li>{@code [YEARS, DAYS]} → valid (skips MONTHS, but YEARS can reach DAYS)</li>
     *   <li>{@code [YEARS, HOURS]} → valid (skips MONTHS, DAYS, HALF_DAYS)</li>
     *   <li>{@code [MONTHS, WEEKS]} → invalid (no path from MONTHS to WEEKS)</li>
     *   <li>{@code [DAYS, YEARS]} → invalid (reversed)</li>
     *   <li>{@code [YEARS, YEARS]} → invalid (not strictly narrowing)</li>
     * </ul>
     *
     * @param grains the grain sequence to validate, widest to narrowest
     * @return {@code true} if every consecutive pair is a valid decomposition hop
     * @throws NullPointerException     if grains or any element is null
     * @throws IllegalArgumentException if grains is empty
     */
    static boolean isValidGrainSequence(final List<ChronoUnit> grains) {
        Objects.requireNonNull(grains, "grains is null");
        if (grains.isEmpty()) {
            throw new IllegalArgumentException("grains is empty");
        }
        if (grains.size() == 1) {
            // Single grain: valid if it's in the decomposition graph
            return !subPath(grains.getFirst()).isEmpty();
        }
        for (int i = 0; i < grains.size() - 1; i++) {
            var wider = Objects.requireNonNull(grains.get(i), "grains[" + i + "] is null");
            var narrower = Objects.requireNonNull(grains.get(i + 1), "grains[" + (i + 1) + "] is null");
            if (wider == narrower) {
                return false; // must be strictly narrowing
            }
            if (!hasPath(wider, narrower)) {
                return false; // no valid decomposition from wider to narrower
            }
        }
        return true;
    }

    private TemporalTilerUtils() {
        throw new AssertionError("instantiation is not allowed");
    }
}
