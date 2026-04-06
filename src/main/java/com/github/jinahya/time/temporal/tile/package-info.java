/**
 * Provides classes for decomposing half-open temporal ranges into boundary-aligned tiles.
 *
 * <p>Given an arbitrary range {@code [start, end)} and a {@link java.time.temporal.TemporalUnit} grain, the library
 * partitions that range into an ordered, non-overlapping, gap-free sequence of tiles. Each tile is either
 * <em>boundary-aligned</em> (spanning exactly one full grain) or <em>partial</em> (a head or tail fragment).
 *
 * <h2>Core classes</h2>
 * <ul>
 *     <li>{@link com.github.jinahya.time.temporal.tile.TemporalTile} &mdash; an immutable tile
 *         ({@code [start, end)} + grain + aligned flag)</li>
 *     <li>{@link com.github.jinahya.time.temporal.tile.TemporalTiler} &mdash; static utility that partitions a range;
 *         accepts a custom truncator, or uses a built-in one for {@link java.time.temporal.ChronoUnit}</li>
 * </ul>
 *
 * <h2>Quick start</h2>
 * <pre>{@code
 * // Tile [Mar 15, Jun 10) by months
 * var tiles = TemporalTiler.tile(
 *     LocalDate.of(2025, 3, 15),
 *     LocalDate.of(2025, 6, 10),
 *     ChronoUnit.MONTHS
 * );
 *
 * // Result:
 * //   [2025-03-15, 2025-04-01)  partial head
 * //   [2025-04-01, 2025-05-01)  aligned
 * //   [2025-05-01, 2025-06-01)  aligned
 * //   [2025-06-01, 2025-06-10)  partial tail
 * }</pre>
 *
 * <h2>Hierarchical tiling</h2>
 * <p>For multi-level decomposition (e.g., year &rarr; month &rarr; day), chain calls on partial tiles:
 * <pre>{@code
 * var yearTiles = TemporalTiler.tile(startDate, endDate, ChronoUnit.YEARS);
 *
 * yearTiles.forEach(tile -> {
 *     if (tile.isAligned()) {
 *         handleYear(tile);
 *     } else {
 *         TemporalTiler.tile(tile.getStart(), tile.getEnd(), ChronoUnit.MONTHS)
 *             .forEach(mTile -> handleMonth(mTile));
 *     }
 * });
 * }</pre>
 *
 * @see com.github.jinahya.time.temporal.tile.TemporalTile
 * @see com.github.jinahya.time.temporal.tile.TemporalTiler
 */
package com.github.jinahya.time.temporal.tile;
