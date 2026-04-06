# temporal-tiler

A Java library for decomposing temporal ranges into boundary-aligned tiles at a single grain level.

## Purpose

Given an arbitrary half-open temporal range `[start, end)` and a `TemporalUnit` grain, produce a list of tiles
partitioning that range. Each tile is either boundary-aligned (full grain) or partial (head/tail). The library does
**not** aggregate, compute, or interpret the tiles — it only decomposes.

For hierarchical tiling (year → month → day), the user chains calls — calling `tile()` on sub-ranges as needed.

## Core Concepts

### Temporal Range

A half-open interval `[start, end)` over any `T extends Temporal & Comparable<? super T>`.

- Supports `LocalTime`, `LocalDate`, `LocalDateTime`, `OffsetDateTime`, `ZonedDateTime`, etc.
- `Temporal` is required (not `TemporalAccessor`) because tiling needs `plus(long, TemporalUnit)` for boundary
  advancement.
- `Comparable` is required for boundary ordering.

### Grain

A `TemporalUnit` that defines the tile width. For `ChronoUnit` grains, `TemporalTiler` provides a built-in
convenience overload. For custom `TemporalUnit` types, the caller supplies a truncator function.

### Tiling

Given `[start, end)` and a grain (e.g., `MONTHS`):

1. Find the next grain boundary after `start` → `alignedStart`
2. Emit head partial: `[start, alignedStart)` if `start` is not on a boundary
3. Emit full aligned tiles: `[alignedStart, +1)`, `[+1, +2)`, ...
4. Emit tail partial: `[lastBoundary, end)` if `end` is not on a boundary

```
input: [2025-03-15, 2025-06-10)
grain: MONTHS

result:
  [2025-03-15, 2025-04-01)  MONTHS  aligned=false  ← partial head
  [2025-04-01, 2025-05-01)  MONTHS  aligned=true   ← full month
  [2025-05-01, 2025-06-01)  MONTHS  aligned=true   ← full month
  [2025-06-01, 2025-06-10)  MONTHS  aligned=false  ← partial tail
```

### Tile Metadata

Each tile knows:

- Its range: `[start, end)`
- Its grain (`TemporalUnit`)
- Whether it is boundary-aligned (full grain) or partial

### Hierarchical Tiling (User-Driven)

For multi-level tiling, the user chains `TemporalTiler.tile()` calls:

```java
var yearTiles = TemporalTiler.tile(startDate, endDate, ChronoUnit.YEARS);

yearTiles.forEach(tile -> {
    if (tile.isAligned()) {
        handleYear(tile);
    } else {
        TemporalTiler.tile(tile.getStart(), tile.getEnd(), ChronoUnit.MONTHS)
            .forEach(mTile -> {
                if (mTile.isAligned()) {
                    handleMonth(mTile);
                } else {
                    TemporalTiler.tile(mTile.getStart(), mTile.getEnd(), ChronoUnit.DAYS)
                        .forEach(dTile -> handleDay(dTile));
                }
            });
    }
});
```

## API

```java
// A single tile (range + metadata)
public final class TemporalTile<T extends Temporal & Comparable<? super T>> {
    T getStart();
    T getEnd();
    TemporalUnit getGrain();
    boolean isAligned();
}

// The tiler — static utility, two overloads
public final class TemporalTiler {

    // Convenience: built-in truncation for ChronoUnit
    static <T extends Temporal & Comparable<? super T>>
    List<TemporalTile<T>> tile(T start, T end, ChronoUnit grain);

    // General-purpose: caller supplies truncator for any TemporalUnit
    static <T extends Temporal & Comparable<? super T>>
    List<TemporalTile<T>> tile(T start, T end, TemporalUnit grain,
                               UnaryOperator<T> truncator);
}
```

### Usage Examples

```java
// --- 1. Simple: partition a range by months ---
TemporalTiler.tile(
    LocalDate.of(2025, 3, 15),
    LocalDate.of(2025, 6, 10),
    ChronoUnit.MONTHS
).forEach(tile -> System.out.printf("[%s, %s) aligned=%b%n",
        tile.getStart(), tile.getEnd(), tile.isAligned()));

// --- 2. Hierarchical: year → month, user-driven ---
var yearTiles = TemporalTiler.tile(startDate, endDate, ChronoUnit.YEARS);

yearTiles.forEach(tile -> {
    if (tile.isAligned()) {
        handleYear(tile);
    } else {
        TemporalTiler.tile(tile.getStart(), tile.getEnd(), ChronoUnit.MONTHS)
            .forEach(mTile -> handleMonth(mTile));
    }
});

// --- 3. Time-based with LocalDateTime ---
TemporalTiler.tile(startDateTime, endDateTime, ChronoUnit.HOURS)
    .forEach(tile -> handleHour(tile));

// --- 4. Custom TemporalUnit with truncator ---
TemporalTiler.tile(start, end, myCustomUnit, value -> truncateToMyUnit(value))
    .forEach(tile -> handle(tile));
```

## Design Principles

- **Decomposition only** — the library produces tiles. It does not cache, aggregate, or interpret them.
- **Stateless** — `TemporalTiler` is a static utility. No instances, no builders, no stored state.
- **Order-preserving** — tiles are emitted in temporal order (left to right).
- **Non-overlapping** — tiles form a complete, gap-free partition of the input range. Half-open intervals `[start, end)`
  guarantee no double-counting at boundaries.
- **Type-safe** — generic over `T extends Temporal & Comparable<? super T>`.
- **Immutable** — `TemporalTile` is immutable.
- **No DST special handling** — `java.time` handles DST transitions correctly. Tiles may vary in duration (23/25-hour
  days) but are always non-overlapping and gap-free.

## Boundary Alignment

A tile is "aligned" when both its start and end fall on natural boundaries of its grain:

- Day-aligned: `T00:00:00` boundaries
- Month-aligned: first day of month
- Year-aligned: first day of year

For date-only types (`LocalDate`), day-alignment is always true for full-day tiles.

## Edge Cases

- **Empty range** (`start >= end`): return empty list.
- **Range smaller than grain**: return a single partial tile.
- **Range exactly one grain**: return a single aligned tile (if boundaries match) or partial.

## Build & Stack

- Java 25+
- No external dependencies (pure `java.time` API)
- JUnit 5 for testing

## Project Structure

```
src/main/java/
  module-info.java
  com/github/jinahya/time/temporal/tile/
    package-info.java
    TemporalTile.java
    TemporalTiler.java

src/test/java/
  com/github/jinahya/time/temporal/tile/
    ChronoTiler_Tile_Test.java
    TemporalTileAssert.java
    TemporalTileTest.java
    _TestConstants.java
```

## Resolved Decisions

- **Static utility, not instances**: `TemporalTiler` has no state. `T` is method-level, inferred from arguments.
- **No `U extends TemporalUnit` type parameter**: grain is stored as `TemporalUnit` in the tile. The unit type
  parameter added no practical value — no standard implementations beyond `ChronoUnit`, and callers never needed the
  concrete unit type in the return.
- **No `& Serializable` bound**: tiling logic never serializes temporal values. All standard `java.time` types are
  serializable anyway.
- **`TemporalTile` is `final`**: no subclassing needed. It's a simple data carrier.
- **Two `tile()` overloads**: `ChronoUnit` convenience (built-in truncation) and general-purpose (`TemporalUnit` +
  caller-supplied truncator).
- **No DST special handling**: `java.time` handles DST transitions correctly.
