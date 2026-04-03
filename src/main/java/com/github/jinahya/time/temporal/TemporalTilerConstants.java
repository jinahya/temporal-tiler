package com.github.jinahya.time.temporal;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Decomposition graph for {@link ChronoUnit}.
 *
 * <p>The graph defines which unit cleanly decomposes into which narrower unit(s)
 * for boundary-aligned tiling. "A → B" means a calendar-aligned span of A always contains a whole number of
 * calendar-aligned spans of B.
 *
 * <pre>{@code
 * MILLENNIA → CENTURIES → DECADES → YEARS → MONTHS → DAYS → HALF_DAYS → HOURS → MINUTES → SECONDS → MILLIS → MICROS → NANOS
 *                                                              ↑
 *                                                       WEEKS ─┘
 * }</pre>
 *
 * <p>{@link ChronoUnit#WEEKS} is a second parent of {@link ChronoUnit#DAYS} but
 * is not reachable from any wider unit (a month is not a whole number of weeks). It can only appear as a ceiling.
 */
final class TemporalTilerConstants {

    /**
     * The main decomposition chain from widest to narrowest. Every consecutive pair (i, i+1) represents a valid parent
     * → child edge.
     *
     * <pre>{@code
     * MILLENNIA → CENTURIES �� DECADES → YEARS → MONTHS → DAYS → HALF_DAYS → HOURS → MINUTES �� SECONDS → MILLIS → MICROS → NANOS
     * }</pre>
     */
    static final List<ChronoUnit> MAIN_CHAIN = List.of(
            ChronoUnit.MILLENNIA,
            ChronoUnit.CENTURIES,
            ChronoUnit.DECADES,
            ChronoUnit.YEARS,
            ChronoUnit.MONTHS,
            ChronoUnit.DAYS,
            ChronoUnit.HALF_DAYS,
            ChronoUnit.HOURS,
            ChronoUnit.MINUTES,
            ChronoUnit.SECONDS,
            ChronoUnit.MILLIS,
            ChronoUnit.MICROS,
            ChronoUnit.NANOS
    );

    /**
     * The orphan chain: {@link ChronoUnit#WEEKS} connects into the main chain at {@link ChronoUnit#DAYS}, but nothing
     * in the main chain decomposes into WEEKS (a month is not a whole number of weeks).
     *
     * <p>WEEKS can only be used as a ceiling. From WEEKS, the path joins the main
     * chain at DAYS and continues downward.
     *
     * <pre>{@code
     * WEEKS → DAYS → HALF_DAYS → HOURS �� ... → NANOS
     * }</pre>
     */
    static final List<ChronoUnit> WEEKS_CHAIN;

    /**
     * All distinct decomposition chains. A valid ceiling→floor path must be a contiguous sub-path of exactly one of
     * these chains.
     */
    static final List<List<ChronoUnit>> ALL_CHAINS;

    /**
     * Adjacency list: each unit maps to its valid narrower children for decomposition. Lookup returns an empty list for
     * leaf units (NANOS) and units not in the graph (ERAS, FOREVER).
     *
     * <p>Example entries:
     * <ul>
     *   <li>{@code YEARS → [MONTHS]}</li>
     *   <li>{@code MONTHS → [DAYS]}</li>
     *   <li>{@code DAYS → [HALF_DAYS]}</li>
     *   <li>{@code WEEKS → [DAYS]}</li>
     *   <li>{@code NANOS → []}</li>
     * </ul>
     */
    static final Map<ChronoUnit, List<ChronoUnit>> CHILDREN;

    /**
     * Adjacency list: each unit maps to its valid wider parents for decomposition. Lookup returns an empty list for
     * root units (MILLENNIA) and units not in the graph.
     *
     * <p>{@link ChronoUnit#DAYS} has two parents: {@code [MONTHS, WEEKS]}.
     */
    static final Map<ChronoUnit, List<ChronoUnit>> PARENTS;

    /**
     * For each {@link ChronoUnit}, the complete sub-path from that unit down to the deepest reachable unit
     * ({@link ChronoUnit#NANOS}).
     *
     * <p>Example entries:
     * <ul>
     *   <li>{@code YEARS → [YEARS, MONTHS, DAYS, HALF_DAYS, HOURS, ..., NANOS]}</li>
     *   <li>{@code WEEKS → [WEEKS, DAYS, HALF_DAYS, HOURS, ..., NANOS]}</li>
     *   <li>{@code NANOS → [NANOS]}</li>
     *   <li>{@code ERAS → []} (not in the decomposition graph)</li>
     * </ul>
     */
    static final Map<ChronoUnit, List<ChronoUnit>> SUB_PATHS;

    static {
        // --- WEEKS_CHAIN ---
        var daysIndex = MAIN_CHAIN.indexOf(ChronoUnit.DAYS);
        var weeksChain = new ArrayList<ChronoUnit>(1 + MAIN_CHAIN.size() - daysIndex);
        weeksChain.add(ChronoUnit.WEEKS);
        weeksChain.addAll(MAIN_CHAIN.subList(daysIndex, MAIN_CHAIN.size()));
        WEEKS_CHAIN = List.copyOf(weeksChain);
        ALL_CHAINS = List.of(MAIN_CHAIN, WEEKS_CHAIN);

        // --- CHILDREN / PARENTS ---
        var children = new EnumMap<ChronoUnit, List<ChronoUnit>>(ChronoUnit.class);
        var parents = new EnumMap<ChronoUnit, List<ChronoUnit>>(ChronoUnit.class);

        // Initialize all units with empty lists
        for (var unit : ChronoUnit.values()) {
            children.put(unit, List.of());
            parents.put(unit, List.of());
        }

        // Main chain edges
        for (int i = 0; i < MAIN_CHAIN.size() - 1; i++) {
            var parent = MAIN_CHAIN.get(i);
            var child = MAIN_CHAIN.get(i + 1);
            children.put(parent, List.of(child));
            parents.put(child, List.of(parent));
        }

        // WEEKS → DAYS: WEEKS is a second parent of DAYS
        children.put(ChronoUnit.WEEKS, List.of(ChronoUnit.DAYS));
        parents.put(ChronoUnit.DAYS, List.of(ChronoUnit.MONTHS, ChronoUnit.WEEKS));

        CHILDREN = Collections.unmodifiableMap(children);
        PARENTS = Collections.unmodifiableMap(parents);

        // --- SUB_PATHS ---
        // For each unit in each chain, the sub-path is from that unit to the end of its chain.
        var subPaths = new EnumMap<ChronoUnit, List<ChronoUnit>>(ChronoUnit.class);
        for (var unit : ChronoUnit.values()) {
            subPaths.put(unit, List.of());
        }
        // Main chain: each unit maps to itself + everything after it
        for (int i = 0; i < MAIN_CHAIN.size(); i++) {
            subPaths.put(MAIN_CHAIN.get(i), MAIN_CHAIN.subList(i, MAIN_CHAIN.size()));
        }
        // WEEKS chain: WEEKS maps to its full chain (DAYS onward already covered by main)
        subPaths.put(ChronoUnit.WEEKS, WEEKS_CHAIN);

        SUB_PATHS = Collections.unmodifiableMap(subPaths);
    }

    /** Prevents instantiation. */
    private TemporalTilerConstants() {
        throw new AssertionError("instantiation is not allowed");
    }
}
