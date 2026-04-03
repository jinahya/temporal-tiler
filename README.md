# temporal-tiler

A Java library for decomposing temporal ranges into boundary-aligned tiles at a single grain level.

## Purpose

Given an arbitrary half-open temporal range `[start, end)` and a `ChronoUnit` grain, produce a list of tiles partitioning that range. Each tile is either boundary-aligned (full grain) or partial (head/tail). The library does **not** aggregate, compute, or interpret the tiles — it only decomposes.

For hierarchical tiling (year → month → day), the user chains tilers — calling `tile()` on sub-ranges as needed.

## How It Works

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

## Usage

### Simple: partition a range by months

```java
TemporalTiler.<LocalDate>of(ChronoUnit.MONTHS)
    .tile(LocalDate.of(2025, 3, 15), LocalDate.of(2025, 6, 10))
    .forEach(tile -> System.out.printf("[%s, %s) aligned=%b%n",
            tile.getStartInclusive(), tile.getEndExclusive(), tile.isAligned()));
```

### Hierarchical: year → month → day (user-driven)

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

### Time-based with LocalDateTime

```java
TemporalTiler.<LocalDateTime>of(ChronoUnit.HOURS)
    .tile(startDateTime, endDateTime)
    .forEach(tile -> handleHour(tile));
```

## API

```java
// A single tile (range + metadata)
public final class TemporalTile<T extends Temporal & Comparable<? super T>> {
    T getStartInclusive();
    T getEndExclusive();
    ChronoUnit getGrain();
    boolean isAligned();
}

// The tiler — one grain, one method
public final class TemporalTiler<T extends Temporal & Comparable<? super T>> {

    static <T extends Temporal & Comparable<? super T>> TemporalTiler<T> of(
            ChronoUnit grain);

    ChronoUnit getGrain();

    List<TemporalTile<T>> tile(T startInclusive, T endExclusive);
}
```

## Design Principles

- **Decomposition only** — produces tiles without caching, aggregating, or interpreting them.
- **Single grain per tiler** — each tiler handles one `ChronoUnit`. Hierarchical tiling is user-driven by chaining tilers.
- **Order-preserving** — tiles are emitted in temporal order.
- **Non-overlapping** — tiles form a complete, gap-free partition of the input range.
- **Type-safe** — generic over `T extends Temporal & Comparable<? super T>`.
- **Immutable** — `TemporalTile` and `TemporalTiler` are immutable.

## Boundary Alignment

A tile is "aligned" when both its start and end fall on natural boundaries of its grain:

- Day-aligned: `T00:00:00` boundaries
- Month-aligned: first day of month
- Year-aligned: first day of year

For date-only types (`LocalDate`), day-alignment is always true for full-day tiles.

## Edge Cases

- **Empty range** (`start >= end`): returns an empty list.
- **Range smaller than grain**: returns a single partial tile.
- **Range exactly one grain**: returns a single aligned tile (if boundaries match) or partial.

## Requirements

- Java 25+
- No external dependencies (pure `java.time` API)
- JUnit 5 for testing
