# temporal-tiler

[![Java CI with Maven](https://github.com/jinahya/temporal-tiler/actions/workflows/maven.yml/badge.svg)](https://github.com/jinahya/temporal-tiler/actions/workflows/maven.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=jinahya_temporal-tiler&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=jinahya_temporal-tiler)

[![Maven Central Version](https://img.shields.io/maven-central/v/io.github.jinahya/temporal-tiler)](https://central.sonatype.com/artifact/io.github.jinahya/temporal-tiler)
[![javadoc](https://javadoc.io/badge2/io.github.jinahya/temporal-tiler/javadoc.svg)](https://javadoc.io/doc/io.github.jinahya/temporal-tiler)


A Java library for decomposing temporal ranges into boundary-aligned tiles at a single grain level.

## Purpose

Given an arbitrary half-open temporal range `[start, end)` and a `TemporalUnit` grain, produce a list of tiles partitioning that range. Each tile is either boundary-aligned (full grain) or partial (head/tail). The library does **not** aggregate, compute, or interpret the tiles — it only decomposes.

For hierarchical tiling (year → month → day), the user chains calls — calling `tile()` on sub-ranges as needed.

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
TemporalTiler.tile(
    LocalDate.of(2025, 3, 15),
    LocalDate.of(2025, 6, 10),
    ChronoUnit.MONTHS
).forEach(tile -> System.out.printf("[%s, %s) aligned=%b%n",
        tile.getStart(), tile.getEnd(), tile.isAligned()));
```

### Hierarchical: year → month → day (user-driven)

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

### Time-based with LocalDateTime

```java
TemporalTiler.tile(startDateTime, endDateTime, ChronoUnit.HOURS)
    .forEach(tile -> handleHour(tile));
```

### Custom TemporalUnit with truncator

```java
TemporalTiler.tile(start, end, myCustomUnit, value -> truncateToMyUnit(value))
    .forEach(tile -> handle(tile));
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

// The tiler — static utility
public final class TemporalTiler {

    // Convenience: built-in truncation for ChronoUnit
    static <T extends Temporal & Comparable<? super T>>
    List<TemporalTile<T>> tile(T start, T end, ChronoUnit grain);

    // General-purpose: caller supplies truncator
    static <T extends Temporal & Comparable<? super T>>
    List<TemporalTile<T>> tile(T start, T end, TemporalUnit grain,
                               UnaryOperator<T> truncator);
}
```

## Design Principles

- **Decomposition only** — produces tiles without caching, aggregating, or interpreting them.
- **Stateless** — `TemporalTiler` is a static utility. No instances, no builders, no stored state.
- **Order-preserving** — tiles are emitted in temporal order.
- **Non-overlapping** — tiles form a complete, gap-free partition of the input range.
- **Type-safe** — generic over `T extends Temporal & Comparable<? super T>`.
- **Immutable** — `TemporalTile` is immutable.

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
