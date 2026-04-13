# temporal-tiler

[![Java CI with Maven](https://github.com/jinahya/temporal-tiler/actions/workflows/maven.yml/badge.svg)](https://github.com/jinahya/temporal-tiler/actions/workflows/maven.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=jinahya_temporal-tiler&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=jinahya_temporal-tiler)

[![Maven Central Version](https://img.shields.io/maven-central/v/io.github.jinahya/temporal-tiler)](https://central.sonatype.com/artifact/io.github.jinahya/temporal-tiler)
[![javadoc](https://javadoc.io/badge2/io.github.jinahya/temporal-tiler/javadoc.svg)](https://javadoc.io/doc/io.github.jinahya/temporal-tiler)


A Java library for decomposing temporal ranges into boundary-aligned tiles at a single grain level.

## Purpose

Given an arbitrary half-open temporal range `[start, end)` and a `ChronoUnit` grain, produce a list of tiles partitioning that range. Each tile is either boundary-aligned (full grain) or partial (head/tail). The library does **not** aggregate, compute, or interpret the tiles — it only decomposes.

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
        tile.start(), tile.end(), tile.aligned()));
```

### Hierarchical: year → month → day (user-driven)

```java
var yearTiles = TemporalTiler.tile(startDate, endDate, ChronoUnit.YEARS);

yearTiles.forEach(tile -> {
    if (tile.aligned()) {
        handleYear(tile);
    } else {
        TemporalTiler.tile(tile, ChronoUnit.MONTHS)
            .forEach(mTile -> {
                if (mTile.aligned()) {
                    handleMonth(mTile);
                } else {
                    TemporalTiler.tile(mTile, ChronoUnit.DAYS)
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

## API

```java
// A single tile (range + metadata)
public final class TemporalTile<T extends Temporal & Comparable<? super T>> {
    T start();
    T end();
    TemporalUnit grain();
    boolean aligned();
}

// The tiler — static utility
public final class TemporalTiler {

    static <T extends Temporal & Comparable<? super T>>
    List<TemporalTile<T>> tile(T start, T end, ChronoUnit grain);

    static <T extends Temporal & Comparable<? super T>>
    List<TemporalTile<T>> tile(TemporalTile<T> tile, ChronoUnit grain);
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

## Supported Type / Grain Combinations

Not every `Temporal` type supports every `ChronoUnit` grain. The temporal type must have enough precision for the requested grain — e.g., `YearMonth` cannot be tiled by `DAYS` because it has no day component.

| Type | NANOS | MICROS | MILLIS | SECONDS | MINUTES | HOURS | HALF_DAYS | DAYS | WEEKS | MONTHS | YEARS | DECADES+ |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `LocalTime` | Y | Y | Y | Y | Y | Y | Y | | | | | |
| `LocalDate` | | | | | | | | Y | Y | Y | Y | Y |
| `LocalDateTime` | Y | Y | Y | Y | Y | Y | Y | Y | Y | Y | Y | Y |
| `OffsetDateTime` | Y | Y | Y | Y | Y | Y | Y | Y | Y | Y | Y | Y |
| `ZonedDateTime` | Y | Y | Y | Y | Y | Y | Y | Y | Y | Y | Y | Y |
| `YearMonth` | | | | | | | | | | Y | Y | Y |
| `Year` | | | | | | | | | | | Y | Y |

> **DECADES+** covers `DECADES`, `CENTURIES`, and `MILLENNIA` — they share the same support pattern.

**Rules of thumb:**

- **Time-based grains** (`NANOS` through `HALF_DAYS`) require `NANO_OF_DAY`. `LocalDate`, `YearMonth`, and `Year` do not support it.
- **`DAYS` and `WEEKS`** require day-level fields (`DAY_OF_WEEK`, `DAY_OF_MONTH`). `YearMonth` and `Year` do not.
- **`MONTHS`** requires `MONTH_OF_YEAR`. `Year` does not.
- **`YEARS` and coarser** require `YEAR`, which all date-capable types support.
- For hierarchical tiling across precision boundaries, convert to a finer type first: `Year` → `YearMonth` → `LocalDate`.

## Edge Cases

- **Empty range** (`start >= end`): returns an empty list.
- **Range smaller than grain**: returns a single partial tile.
- **Range exactly one grain**: returns a single aligned tile (if boundaries match) or partial.

## Requirements

- Java 25+
- No external dependencies (pure `java.time` API)
- JUnit 5 for testing
