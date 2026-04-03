# temporal-tiler

A Java library for decomposing temporal ranges into boundary-aligned tiles at a single grain level.

## Purpose

Given an arbitrary half-open temporal range `[start, end)` and a `ChronoUnit` grain, produce a list of tiles partitioning that range. Each tile is either boundary-aligned (full grain) or partial (head/tail). The library does **not** aggregate, compute, or interpret the tiles — it only decomposes.

For hierarchical tiling (year → month → day), the user chains tilers — calling `tile()` on sub-ranges as needed.

## Core Concepts

### Temporal Range

A half-open interval `[startInclusive, endExclusive)` over any `T extends Temporal & Comparable<? super T>`.

- Supports `Instant`, `LocalDate`, `LocalDateTime`, `OffsetDateTime`, `ZonedDateTime`, etc.
- `Temporal` is required (not `TemporalAccessor`) because tiling needs `plus(long, TemporalUnit)` for boundary advancement.
- `Comparable` is required for boundary ordering.

### Grain

A single `ChronoUnit` that defines the tile width. The tiler partitions the range into tiles of this unit.

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
- Its range: `[startInclusive, endExclusive)`
- Its grain (`ChronoUnit`)
- Whether it is boundary-aligned (full grain) or partial

### Hierarchical Tiling (User-Driven)

For multi-level tiling, the user creates multiple tilers and chains them:

```java
var monthTiler = TemporalTiler.<LocalDate>of(ChronoUnit.MONTHS);
var dayTiler   = TemporalTiler.<LocalDate>of(ChronoUnit.DAYS);

monthTiler.tile(startDate, endDate).forEach(tile -> {
    if (tile.isAligned()) {
        handleMonth(tile);  // full month — user decides (cache, compute, etc.)
    } else {
        // Partial month — tile into days
        dayTiler.tile(tile.getStartInclusive(), tile.getEndExclusive())
            .forEach(dayTile -> handleDay(dayTile));
    }
});
```

For three levels:

```java
var yearTiler  = TemporalTiler.<LocalDate>of(ChronoUnit.YEARS);
var monthTiler = TemporalTiler.<LocalDate>of(ChronoUnit.MONTHS);
var dayTiler   = TemporalTiler.<LocalDate>of(ChronoUnit.DAYS);

yearTiler.tile(startDate, endDate).forEach(tile -> {
    if (tile.isAligned()) {
        handleYear(tile);
    } else {
        monthTiler.tile(tile.getStartInclusive(), tile.getEndExclusive()).forEach(mTile -> {
            if (mTile.isAligned()) {
                handleMonth(mTile);
            } else {
                dayTiler.tile(mTile.getStartInclusive(), mTile.getEndExclusive())
                    .forEach(dTile -> handleDay(dTile));
            }
        });
    }
});
```

## API Sketch

```java
// A single tile (range + metadata)
public final class TemporalTile<T extends Temporal & Comparable<? super T>> {
    T getStartInclusive();
    T getEndExclusive();
    ChronoUnit getGrain();
    boolean isAligned();
}

// Controls traversal in hierarchical tiling
public enum TileVisitResult {
    CONTINUE,       // decompose into narrower grains
    SKIP_CHILDREN   // accept as leaf, do not decompose further
}

// The tiler — one grain, one method
public final class TemporalTiler<T extends Temporal & Comparable<? super T>> {

    static <T extends Temporal & Comparable<? super T>> TemporalTiler<T> of(
            ChronoUnit grain);

    ChronoUnit getGrain();

    List<TemporalTile<T>> tile(T startInclusive, T endExclusive);
}
```

### Usage Examples

```java
// --- 1. Simple: partition a range by months ---

TemporalTiler.<LocalDate>of(ChronoUnit.MONTHS)
    .tile(LocalDate.of(2025, 3, 15), LocalDate.of(2025, 6, 10))
    .forEach(tile -> System.out.printf("[%s, %s) aligned=%b%n",
            tile.getStartInclusive(), tile.getEndExclusive(), tile.isAligned()));

// --- 2. Hierarchical: year → month, user-driven ---

var yearTiler  = TemporalTiler.<LocalDate>of(ChronoUnit.YEARS);
var monthTiler = TemporalTiler.<LocalDate>of(ChronoUnit.MONTHS);

yearTiler.tile(startDate, endDate).forEach(tile -> {
    if (tile.isAligned()) {
        handleYear(tile);
    } else {
        monthTiler.tile(tile.getStartInclusive(), tile.getEndExclusive())
            .forEach(mTile -> handleMonth(mTile));
    }
});

// --- 3. Time-based with LocalDateTime ---

TemporalTiler.<LocalDateTime>of(ChronoUnit.HOURS)
    .tile(startDateTime, endDateTime)
    .forEach(tile -> handleHour(tile));
```

## Design Principles

- **Decomposition only** — the library produces tiles. It does not cache, aggregate, or interpret them.
- **Single grain per tiler** — each tiler handles one `ChronoUnit`. Hierarchical tiling is user-driven by chaining tilers.
- **Order-preserving** — tiles are emitted in temporal order (left to right).
- **Non-overlapping** — tiles form a complete, gap-free partition of the input range. Half-open intervals `[start, end)` guarantee no double-counting at boundaries.
- **Type-safe** — generic over `T extends Temporal & Comparable<? super T>`. No raw casts.
- **Immutable** — `TemporalTile` and `TemporalTiler` are immutable.
- **No DST special handling** — `java.time` handles DST transitions correctly. Tiles may vary in duration (23/25-hour days) but are always non-overlapping and gap-free.

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
  com/github/jinahya/time/temporal/
    TemporalTile.java
    TemporalTiler.java
    TemporalTilerConstants.java
    TemporalTilerHelper.java
    TemporalTilerUtils.java
    TileVisitResult.java

src/test/java/
  com/github/jinahya/time/temporal/
    TemporalTilerTest.java
    TemporalTilerUtilsTest.java
    _TestConstants.java
```

## Resolved Decisions

- **Single grain per tiler**: simplest possible API. Hierarchical tiling is user-driven by chaining tilers on partial tiles.
- **Grains use `ChronoUnit`** (not `TemporalAmount`). `ChronoUnit` implements `TemporalUnit` and provides `isSupportedBy()`, `between()`, `addTo()`, and duration-based ordering.
- **Alignment detection** uses `TemporalUnit.between()` — no custom predicates needed.
- **No DST special handling**: `java.time` handles DST transitions correctly.
