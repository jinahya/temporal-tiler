/**
 * Provides classes for decomposing half-open temporal ranges into boundary-aligned tiles.
 *
 * <p>Given an arbitrary range {@code [start, end)} and a {@link java.time.temporal.TemporalUnit} grain, the library
 * partitions that range into an ordered, non-overlapping, gap-free sequence of tiles. Each tile is either
 * <em>boundary-aligned</em> (spanning exactly one full grain) or <em>partial</em> (a head or tail fragment).
 *
 * <h2>Core abstractions</h2>
 * <ul>
 *     <li>{@link com.github.jinahya.time.temporal.tile.TemporalTile} &mdash; abstract base for a single tile
 *         ({@code [start, end)} + grain + aligned flag)</li>
 *     <li>{@link com.github.jinahya.time.temporal.tile.TemporalTiler} &mdash; abstract base tiler; holds a range and
 *     grain,
 *         produces tiles via {@link com.github.jinahya.time.temporal.tile.TemporalTiler#tile()}</li>
 * </ul>
 *
 * <h2>Concrete implementations for {@link java.time.temporal.ChronoUnit}</h2>
 * <ul>
 *     <li>{@link com.github.jinahya.time.temporal.tile.ChronoTile} &mdash;
 *         {@code TemporalTile<T, ChronoUnit>}</li>
 *     <li>{@link com.github.jinahya.time.temporal.tile.ChronoTiler} &mdash;
 *         {@code TemporalTiler<T, ChronoUnit>}, with a concrete
 *         {@link com.github.jinahya.time.temporal.tile.ChronoTiler.Builder Builder} and static convenience
 *         methods</li>
 * </ul>
 *
 * <h2>Quick start</h2>
 * <pre>{@code
 * // Tile [Mar 15, Jun 10) by months
 * var tiles = new ChronoTiler.Builder<LocalDate>()
 *     .start(LocalDate.of(2025, 3, 15))
 *     .end(LocalDate.of(2025, 6, 10))
 *     .grain(ChronoUnit.MONTHS)
 *     .build()
 *     .tile();
 *
 * // Result:
 * //   [2025-03-15, 2025-04-01)  partial head
 * //   [2025-04-01, 2025-05-01)  aligned
 * //   [2025-05-01, 2025-06-01)  aligned
 * //   [2025-06-01, 2025-06-10)  partial tail
 * }</pre>
 *
 * <h2>Hierarchical tiling</h2>
 * <p>For multi-level decomposition (e.g., year &rarr; month &rarr; day), chain tilers on partial tiles:
 * <pre>{@code
 * var yearTiles = new ChronoTiler.Builder<LocalDate>()
 *     .start(startDate).end(endDate).grain(ChronoUnit.YEARS)
 *     .build().tile();
 *
 * yearTiles.forEach(tile -> {
 *     if (tile.isAligned()) {
 *         handleYear(tile);
 *     } else {
 *         new ChronoTiler.Builder<LocalDate>()
 *             .start(tile.getStart()).end(tile.getEnd()).grain(ChronoUnit.MONTHS)
 *             .build().tile()
 *             .forEach(mTile -> handleMonth(mTile));
 *     }
 * });
 * }</pre>
 *
 * @see com.github.jinahya.time.temporal.tile.TemporalTile
 * @see com.github.jinahya.time.temporal.tile.TemporalTiler
 * @see com.github.jinahya.time.temporal.tile.ChronoTile
 * @see com.github.jinahya.time.temporal.tile.ChronoTiler
 */
package com.github.jinahya.time.temporal.tile;