# temporal-tiler

A Java library for decomposing temporal ranges into hierarchical, boundary-aligned, non-overlapping sub-ranges (tiles).

## Purpose

Given an arbitrary half-open temporal range `[start, end)`, produce a sequence of sub-ranges aligned to calendar boundaries (year, month, day, etc.) at varying granularity. The library does **not** aggregate, compute, or interpret the tiles — it only decomposes. Users decide what to do with each tile (cache lookup, parallel computation, sequential processing, etc.).

## Core Concepts

### Temporal Range

A half-open interval `[startInclusive, endExclusive)` over any `T extends Temporal & Comparable<? super T>`.

- Supports `Instant`, `LocalDate`, `LocalDateTime`, `OffsetDateTime`, `ZonedDateTime`, etc.
- `Temporal` is required (not `TemporalAccessor`) because tiling needs `plus(long, TemporalUnit)` for boundary advancement.
- `Comparable` is required for boundary ordering.

### Grain

A `TemporalUnit` (typically `ChronoUnit`) that defines a tile width.

`TemporalUnit` is preferred over `TemporalAmount` because it provides:
- `addTo(temporal, amount)` — advance boundaries
- `between(start, end)` — count whole units between two temporals
- `isSupportedBy(temporal)` — runtime compatibility check (e.g., `ChronoUnit.HOURS.isSupportedBy(LocalDate)` returns `false`)
- `isDateBased()` / `isTimeBased()` — classify grains
- `getDuration()` — natural ordering from widest to narrowest

### Grain List

A tiler is configured with an explicit list of `ChronoUnit` values from widest to narrowest. The user controls exactly which grain levels exist — no automatic intermediate units.

```java
// User specifies: YEARS → MONTHS → DAYS (no HALF_DAYS, no WEEKS)
var tiler = TemporalTiler.<LocalDate>of(
    ChronoUnit.YEARS, ChronoUnit.MONTHS, ChronoUnit.DAYS
);

// User skips DAYS — goes directly from MONTHS to HOURS
var tiler2 = TemporalTiler.<LocalDateTime>of(
    ChronoUnit.MONTHS, ChronoUnit.HOURS
);
```

The library validates each consecutive pair is reachable in the `ChronoUnit` decomposition graph (i.e., the wider unit can cleanly decompose into the narrower unit), but the user decides which levels to include.

The grain list declares **intent**: the first grain is the user's preferred resolution, the rest are fallbacks for unaligned edges. `[YEARS, MONTHS, DAYS]` means "give me years where possible, months where not, days at the edges."

### Hierarchical Tiling

The tiler aligns to the widest grain first. **Aligned tiles stay at their grain level. Only partial (unaligned) head/tail tiles are decomposed further** into the next narrower grain. The output is a mixed-grain stream — no overlap, no gaps, each tile at the widest grain it can align to.

```
input:  [2025-03-15, 2027-06-10)
grains: [YEARS, MONTHS, DAYS]

result:
  [2025-03-15, 2025-03-16)  DAYS    ← partial head (can't align to month)
  [2025-03-16, 2025-03-17)  DAYS
  ...
  [2025-03-31, 2025-04-01)  DAYS
  [2025-04-01, 2025-05-01)  MONTHS  ← aligned month (stays at month level)
  ...
  [2025-12-01, 2026-01-01)  MONTHS
  [2026-01-01, 2027-01-01)  YEARS   ← aligned full year
  [2027-01-01, 2027-02-01)  MONTHS  ← aligned month
  ...
  [2027-05-01, 2027-06-01)  MONTHS
  [2027-06-01, 2027-06-02)  DAYS    ← partial tail
  ...
  [2027-06-09, 2027-06-10)  DAYS
```

### Tile Metadata

Each tile knows:
- Its range: `[startInclusive, endExclusive)`
- Its grain level (which `ChronoUnit` produced it)
- Whether it is boundary-aligned (full grain) or a partial (head/tail remainder)
- Its depth in the grain hierarchy (0 = widest grain)

### On-Demand Decomposition (`tile.decompose()`)

A tile can be further decomposed into the remaining grains below it. The tile holds a reference back to its parent tiler and knows its position in the grain list.

```java
tiler.tile(startDate, endDate).forEach(tile -> {
    switch (tile.getGrain()) {
        case YEARS  -> handleYear(tile);
        case MONTHS -> {
            if (needDayDetail(tile)) {
                tile.decompose().forEach(dayTile -> handleDay(dayTile));
            } else {
                handleMonth(tile);
            }
        }
        case DAYS   -> handleDay(tile);
    }
});
```

`tile.decompose()` returns `Stream<TemporalTile<T>>` — tiles the range of this tile using the remaining grains below it from the original tiler. For a MONTHS tile from a `[YEARS, MONTHS, DAYS]` tiler, `decompose()` tiles it with `[DAYS]`.

This is how users handle "I got a full month but I need day-level detail for this one." No visitor, no context object — the tile itself is the entry point for further tiling.

## API Sketch

```java
// Type bound for temporal values
// T extends Temporal & Comparable<? super T>

// A single tile (range + metadata + decompose)
public final class TemporalTile<T extends Temporal & Comparable<? super T>> {
    T getStartInclusive();
    T getEndExclusive();
    ChronoUnit getGrain();          // the grain that produced this tile
    boolean isAligned();            // true if boundary-aligned (full grain)
    int getDepth();                 // 0 = widest grain, increments toward narrowest

    // On-demand decomposition: tile this range with the remaining grains below
    // For a MONTHS tile from [YEARS, MONTHS, DAYS] → tiles with [DAYS]
    // Returns empty stream if this tile is already at the narrowest grain
    Stream<TemporalTile<T>> decompose();
}

// The tiler — configured with a user-specified grain list
public final class TemporalTiler<T extends Temporal & Comparable<? super T>> {

    // Factory — grains from widest to narrowest
    // Each consecutive pair must be reachable in the ChronoUnit decomposition graph
    static <T extends Temporal & Comparable<? super T>> TemporalTiler<T> of(
            ChronoUnit... grains);

    static <T extends Temporal & Comparable<? super T>> TemporalTiler<T> of(
            List<ChronoUnit> grains);

    List<ChronoUnit> getGrains();

    // Tile the range — returns mixed-grain stream in temporal order
    // Aligned tiles stay at their grain level; only partials decompose further
    Stream<TemporalTile<T>> tile(T startInclusive, T endExclusive);
}
```

### Usage Examples

```java
var tiler = TemporalTiler.<LocalDate>of(
    ChronoUnit.YEARS, ChronoUnit.MONTHS, ChronoUnit.DAYS
);

// --- 1. Simple: iterate all tiles ---

tiler.tile(LocalDate.of(2025, 3, 15), LocalDate.of(2027, 6, 10))
    .forEach(tile -> System.out.printf("[%s, %s) %s aligned=%b%n",
            tile.getStartInclusive(), tile.getEndExclusive(),
            tile.getGrain(), tile.isAligned()));

// --- 2. Dispatch by grain level ---

tiler.tile(startDate, endDate).forEach(tile -> {
    switch (tile.getGrain()) {
        case YEARS  -> handleYear(tile);    // user's method, maybe cached
        case MONTHS -> handleMonth(tile);
        case DAYS   -> handleDay(tile);
    }
});

// --- 3. On-demand decomposition: drill into a specific month ---

tiler.tile(startDate, endDate).forEach(tile -> {
    switch (tile.getGrain()) {
        case YEARS  -> handleYear(tile);
        case MONTHS -> {
            if (needDayDetail(tile)) {
                // Decompose this month into DAYS (remaining grains)
                tile.decompose().forEach(dayTile -> handleDay(dayTile));
            } else {
                handleMonth(tile);
            }
        }
        case DAYS   -> handleDay(tile);
    }
});

// --- 4. Collect to list, group by grain ---

var tiles = tiler.tile(startDate, endDate).toList();
var byGrain = tiles.stream().collect(Collectors.groupingBy(TemporalTile::getGrain));

// --- 5. Single-grain tiler ---

var monthlyTiler = TemporalTiler.<LocalDate>of(ChronoUnit.MONTHS);

// --- 6. Time-based grains with LocalDateTime ---

var hourlyTiler = TemporalTiler.<LocalDateTime>of(
    ChronoUnit.MONTHS, ChronoUnit.DAYS, ChronoUnit.HOURS
);
```

## Design Principles

- **Decomposition only** — the library produces tiles. It does not cache, aggregate, or interpret them.
- **Mixed-grain output** — aligned tiles stay at their grain level; only partial edges decompose further. The stream contains tiles at different grain levels, tagged with metadata.
- **On-demand drill-down** — `tile.decompose()` lets the user break an aligned tile into finer grains without a visitor or context. The tile carries everything needed.
- **User-specified grains** — the tiler takes an explicit list of `ChronoUnit` values (widest to narrowest). The user controls exactly which grain levels exist. The library validates each consecutive pair is reachable in the decomposition graph but does not inject intermediate units.
- **Order-preserving** — tiles are emitted in temporal order (left to right).
- **Non-overlapping** — tiles form a complete, gap-free partition of the input range. Half-open intervals `[start, end)` guarantee no double-counting at boundaries.
- **Type-safe** — generic over `T extends Temporal & Comparable<? super T>`. No raw casts.
- **Immutable** — `TemporalTile` and `TemporalTiler` are immutable.

## Boundary Alignment

A tile is "aligned" when both its start and end fall on natural boundaries of its grain:

- Day-aligned: `T00:00:00` boundaries
- Month-aligned: first day of month
- Year-aligned: first day of year

For date-only types (`LocalDate`), day-alignment is always true for full-day tiles.

Alignment can be detected using `TemporalUnit`: check if `unit.between(origin, value)` yields `n` such that `origin.plus(n, unit).equals(value)`. For `ChronoUnit`, this is well-defined for all standard temporal types.

## Edge Cases

- **Empty range** (`start >= end`): return empty stream (or throw — design decision).
- **Range smaller than narrowest grain**: return a single partial tile.
- **Single grain**: no hierarchy — just partition into grain-sized tiles with a head/tail partial.
- **Invalid grain sequence**: rejected at construction if any consecutive pair is unreachable in the decomposition graph (e.g., `[MONTHS, WEEKS]`) or not strictly narrowing.

## Build & Stack

- Java 25+
- No external dependencies (pure `java.time` API)
- JUnit 5 for testing

## Project Structure

```
src/main/java/
  com/github/jinahya/time/temporal/
    TemporalTile.java
    TemporalTiler.java
    TemporalTilerConstants.java
    TemporalTilerUtils.java
    ChronoUnitMap.java

src/test/java/
  com/github/jinahya/time/temporal/
    TemporalTileTest.java
    TemporalTilerTest.java
    TemporalTilerUtilsTest.java
    ChronoUnitMapTest.java
```

## Resolved Decisions

- **Grains use `ChronoUnit`** (not `TemporalAmount`). `ChronoUnit` implements `TemporalUnit` and provides `isSupportedBy()`, `between()`, `addTo()`, and duration-based ordering.
- **User-specified grain list**: the user provides an explicit list of `ChronoUnit` values from widest to narrowest. Each consecutive pair is validated as reachable in the decomposition graph. No automatic intermediate units — the user controls exactly which levels to tile at.
- **Alignment detection** uses `TemporalUnit.between()` — no custom predicates needed.
- **Simple stream API**: `tile()` returns `Stream<TemporalTile<T>>` — a mixed-grain stream where aligned tiles stay at their grain, partials decompose further. No visitor, no collector, no context.
- **`tile.decompose()`**: on-demand drill-down. The tile holds a reference to its parent tiler and knows the remaining grains. User calls `decompose()` to break an aligned tile into finer grains. No new types needed.
- **No DST special handling**: `java.time` handles DST transitions correctly. Tiles may vary in duration (23/25-hour days) but are always non-overlapping and gap-free.

## Open Questions

- **Mixed date/time units**: a grain list like `[MONTHS, DAYS, HOURS]` requires the `Temporal` type to support all units. `LocalDateTime` does; `LocalDate` does not for `HOURS`. Validate all grains via `isSupportedBy()` at construction or fail at tile time?
- **Existing `TemporalSlice`**: `com.github.jinahya.time.temporal.TemporalSlice` in `jinahya-se` provides range logic. Decide whether to depend on it, inline it, or replace it.
